CREATE TABLE group_orders (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 host_user_id BIGINT NOT NULL,
 invite_token VARCHAR(36) NOT NULL UNIQUE,
 status VARCHAR(20) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 order_id BIGINT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_group_host FOREIGN KEY(host_user_id) REFERENCES users(id),
 CONSTRAINT fk_group_order FOREIGN KEY(order_id) REFERENCES orders(id),
 INDEX ix_group_host_status(host_user_id,status)
);
CREATE TABLE group_order_members (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 group_order_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 display_name VARCHAR(100) NOT NULL,
 items_json TEXT NOT NULL,
 CONSTRAINT uq_group_member UNIQUE(group_order_id,user_id),
 CONSTRAINT fk_group_member_group FOREIGN KEY(group_order_id) REFERENCES group_orders(id),
 CONSTRAINT fk_group_member_user FOREIGN KEY(user_id) REFERENCES users(id)
);
ALTER TABLE carts ADD COLUMN group_order_id BIGINT NULL;
ALTER TABLE carts ADD CONSTRAINT fk_cart_group FOREIGN KEY(group_order_id) REFERENCES group_orders(id);
ALTER TABLE orders ADD COLUMN group_order_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN group_participants INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN group_discount DECIMAL(14,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD CONSTRAINT fk_order_group FOREIGN KEY(group_order_id) REFERENCES group_orders(id);
