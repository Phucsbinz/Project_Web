-- Identity-owned tables. External catalog/order foreign keys are proposed separately for integration.
CREATE TABLE otp_challenges (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT, email VARCHAR(254) NOT NULL, purpose VARCHAR(20) NOT NULL,
otp_hash VARCHAR(64) NOT NULL, attempts INT NOT NULL, created_at DATETIME(6) NOT NULL,
expires_at DATETIME(6) NOT NULL, consumed_at DATETIME(6), revoked_at DATETIME(6),
reset_grant_hash VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
CONSTRAINT fk_otp_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE store_memberships (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, store_id BIGINT NOT NULL, member_role VARCHAR(20) NOT NULL,
is_active BOOLEAN NOT NULL, assigned_by BIGINT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
CONSTRAINT uq_membership UNIQUE(user_id,store_id), CONSTRAINT fk_membership_user FOREIGN KEY(user_id) REFERENCES users(id),
CONSTRAINT fk_membership_actor FOREIGN KEY(assigned_by) REFERENCES users(id)
);

CREATE TABLE user_addresses (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, recipient_name VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL,
address_detail VARCHAR(500) NOT NULL, is_default BOOLEAN NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
CONSTRAINT fk_address_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE loyalty_accounts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL UNIQUE, balance INT NOT NULL, debt INT NOT NULL, lifetime_earned INT NOT NULL,
updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
CONSTRAINT ck_points_nonnegative CHECK(balance>=0 AND debt>=0 AND lifetime_earned>=0),
CONSTRAINT fk_loyalty_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE loyalty_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 account_id BIGINT NOT NULL, order_id BIGINT NOT NULL, type VARCHAR(20) NOT NULL,
points_delta INT NOT NULL, balance_after INT NOT NULL, debt_after INT NOT NULL, created_at DATETIME(6) NOT NULL,
CONSTRAINT uq_loyalty_event UNIQUE(order_id,type), CONSTRAINT fk_ledger_account FOREIGN KEY(account_id) REFERENCES loyalty_accounts(id)
);

CREATE TABLE audit_logs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 actor_id BIGINT NOT NULL, action VARCHAR(50) NOT NULL, entity_type VARCHAR(50) NOT NULL, entity_id BIGINT NOT NULL,
summary VARCHAR(1000) NOT NULL, created_at DATETIME(6) NOT NULL, CONSTRAINT fk_audit_actor FOREIGN KEY(actor_id) REFERENCES users(id)
);

CREATE TABLE product_favorites (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
CONSTRAINT uq_favorite UNIQUE(user_id,product_id), CONSTRAINT fk_favorite_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE product_views (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, last_viewed_at DATETIME(6) NOT NULL, view_count INT NOT NULL,
CONSTRAINT uq_product_view UNIQUE(user_id,product_id), CONSTRAINT fk_view_user FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE reviews (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 author_id BIGINT NOT NULL, order_item_id BIGINT NOT NULL UNIQUE, product_id BIGINT NOT NULL,
stars INT NOT NULL, content VARCHAR(2000), status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
version BIGINT NOT NULL DEFAULT 0, CONSTRAINT ck_review_stars CHECK(stars BETWEEN 1 AND 5), CONSTRAINT fk_review_author FOREIGN KEY(author_id) REFERENCES users(id)
);

CREATE TABLE review_media (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 review_id BIGINT NOT NULL, secure_url VARCHAR(1000) NOT NULL, public_id VARCHAR(255) NOT NULL,
resource_type VARCHAR(10) NOT NULL, display_order INT NOT NULL, CONSTRAINT fk_media_review FOREIGN KEY(review_id) REFERENCES reviews(id)
);

CREATE TABLE product_comments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 author_id BIGINT NOT NULL, product_id BIGINT NOT NULL, order_item_id BIGINT NOT NULL,
content VARCHAR(2000) NOT NULL, status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
version BIGINT NOT NULL DEFAULT 0, CONSTRAINT fk_comment_author FOREIGN KEY(author_id) REFERENCES users(id)
);

CREATE TABLE comment_media (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 comment_id BIGINT NOT NULL, secure_url VARCHAR(1000) NOT NULL, public_id VARCHAR(255) NOT NULL,
resource_type VARCHAR(10) NOT NULL, display_order INT NOT NULL, CONSTRAINT fk_media_comment FOREIGN KEY(comment_id) REFERENCES product_comments(id)
);

CREATE INDEX ix_otp_email_purpose ON otp_challenges(email,purpose,id);
CREATE INDEX ix_otp_email_created ON otp_challenges(email,created_at);
CREATE INDEX ix_address_owner ON user_addresses(user_id,is_default);
CREATE INDEX ix_membership_scope ON store_memberships(user_id,is_active,store_id);
CREATE INDEX ix_ledger_account ON loyalty_transactions(account_id,id);
CREATE INDEX ix_review_product ON reviews(product_id,status,id);
CREATE INDEX ix_comment_product ON product_comments(product_id,status,id);
