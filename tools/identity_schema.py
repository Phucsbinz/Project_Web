from pathlib import Path

tables = {
'otp_challenges': '''user_id BIGINT, email VARCHAR(254) NOT NULL, purpose VARCHAR(20) NOT NULL,
otp_hash VARCHAR(64) NOT NULL, attempts INT NOT NULL, created_at DATETIME(6) NOT NULL,
expires_at DATETIME(6) NOT NULL, consumed_at DATETIME(6), revoked_at DATETIME(6),
reset_grant_hash VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
CONSTRAINT fk_otp_user FOREIGN KEY(user_id) REFERENCES users(id)''',
'store_memberships': '''user_id BIGINT NOT NULL, store_id BIGINT NOT NULL, member_role VARCHAR(20) NOT NULL,
is_active BOOLEAN NOT NULL, assigned_by BIGINT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
CONSTRAINT uq_membership UNIQUE(user_id,store_id), CONSTRAINT fk_membership_user FOREIGN KEY(user_id) REFERENCES users(id),
CONSTRAINT fk_membership_actor FOREIGN KEY(assigned_by) REFERENCES users(id)''',
'user_addresses': '''user_id BIGINT NOT NULL, recipient_name VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL,
address_detail VARCHAR(500) NOT NULL, is_default BOOLEAN NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
CONSTRAINT fk_address_user FOREIGN KEY(user_id) REFERENCES users(id)''',
'loyalty_accounts': '''user_id BIGINT NOT NULL UNIQUE, balance INT NOT NULL, debt INT NOT NULL, lifetime_earned INT NOT NULL,
updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
CONSTRAINT ck_points_nonnegative CHECK(balance>=0 AND debt>=0 AND lifetime_earned>=0),
CONSTRAINT fk_loyalty_user FOREIGN KEY(user_id) REFERENCES users(id)''',
'loyalty_transactions': '''account_id BIGINT NOT NULL, order_id BIGINT NOT NULL, type VARCHAR(20) NOT NULL,
points_delta INT NOT NULL, balance_after INT NOT NULL, debt_after INT NOT NULL, created_at DATETIME(6) NOT NULL,
CONSTRAINT uq_loyalty_event UNIQUE(order_id,type), CONSTRAINT fk_ledger_account FOREIGN KEY(account_id) REFERENCES loyalty_accounts(id)''',
'audit_logs': '''actor_id BIGINT NOT NULL, action VARCHAR(50) NOT NULL, entity_type VARCHAR(50) NOT NULL, entity_id BIGINT NOT NULL,
summary VARCHAR(1000) NOT NULL, created_at DATETIME(6) NOT NULL, CONSTRAINT fk_audit_actor FOREIGN KEY(actor_id) REFERENCES users(id)''',
'product_favorites': '''user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
CONSTRAINT uq_favorite UNIQUE(user_id,product_id), CONSTRAINT fk_favorite_user FOREIGN KEY(user_id) REFERENCES users(id)''',
'product_views': '''user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, last_viewed_at DATETIME(6) NOT NULL, view_count INT NOT NULL,
CONSTRAINT uq_product_view UNIQUE(user_id,product_id), CONSTRAINT fk_view_user FOREIGN KEY(user_id) REFERENCES users(id)''',
'reviews': '''author_id BIGINT NOT NULL, order_item_id BIGINT NOT NULL UNIQUE, product_id BIGINT NOT NULL,
stars INT NOT NULL, content VARCHAR(2000), status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
version BIGINT NOT NULL DEFAULT 0, CONSTRAINT ck_review_stars CHECK(stars BETWEEN 1 AND 5), CONSTRAINT fk_review_author FOREIGN KEY(author_id) REFERENCES users(id)''',
'review_media': '''review_id BIGINT NOT NULL, secure_url VARCHAR(1000) NOT NULL, public_id VARCHAR(255) NOT NULL,
resource_type VARCHAR(10) NOT NULL, display_order INT NOT NULL, CONSTRAINT fk_media_review FOREIGN KEY(review_id) REFERENCES reviews(id)''',
'product_comments': '''author_id BIGINT NOT NULL, product_id BIGINT NOT NULL, order_item_id BIGINT NOT NULL,
content VARCHAR(2000) NOT NULL, status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
version BIGINT NOT NULL DEFAULT 0, CONSTRAINT fk_comment_author FOREIGN KEY(author_id) REFERENCES users(id)''',
'comment_media': '''comment_id BIGINT NOT NULL, secure_url VARCHAR(1000) NOT NULL, public_id VARCHAR(255) NOT NULL,
resource_type VARCHAR(10) NOT NULL, display_order INT NOT NULL, CONSTRAINT fk_media_comment FOREIGN KEY(comment_id) REFERENCES product_comments(id)'''
}
sql='-- Identity-owned tables. External catalog/order foreign keys are proposed separately for integration.\n'
for name, columns in tables.items():
    sql += f'CREATE TABLE {name} (\n id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n {columns}\n);\n\n'
sql += '''CREATE INDEX ix_otp_email_purpose ON otp_challenges(email,purpose,id);
CREATE INDEX ix_otp_email_created ON otp_challenges(email,created_at);
CREATE INDEX ix_address_owner ON user_addresses(user_id,is_default);
CREATE INDEX ix_membership_scope ON store_memberships(user_id,is_active,store_id);
CREATE INDEX ix_ledger_account ON loyalty_transactions(account_id,id);
CREATE INDEX ix_review_product ON reviews(product_id,status,id);
CREATE INDEX ix_comment_product ON product_comments(product_id,status,id);
'''
Path('src/main/resources/db/migration/mysql/V2026101701__identity_membership.sql').write_text(sql,encoding='utf-8')

# Entity metadata lets Hibernate validate every identity-owned column on MySQL.
import re
for name, columns in tables.items():
    class_name=''.join(word.title() for word in name.split('_'))
    fields=[]
    for column in columns.split(','):
        match=re.match(r'\s*(\w+)\s+(BIGINT|INT|BOOLEAN|DATETIME\(6\)|VARCHAR\((\d+)\))\s*(NOT NULL)?',column)
        if not match:
            continue
        col,kind,length,required=match.groups()
        field=col.split('_')[0]+''.join(s.title() for s in col.split('_')[1:])
        java_type={'BIGINT':'Long','INT':'Integer','BOOLEAN':'Boolean','DATETIME(6)':'Instant'}.get(kind,'String')
        annotation='@Version ' if col=='version' else ''
        annotation+=f'@Column(name="{col}",nullable={"false" if required else "true"}'
        if length: annotation+=f',length={length}'
        annotation+=')'
        fields.append(f'    {annotation} private {java_type} {field};')
    content=f'''package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="{name}")
public class {class_name} {{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
{chr(10).join(fields)}
    protected {class_name}() {{}}
    public Long getId() {{ return id; }}
}}
'''
    dest=Path('src/main/java/com/utetra/identity/entity')/f'{class_name}.java'
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(content,encoding='utf-8')
