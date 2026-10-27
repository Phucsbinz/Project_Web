CREATE TABLE local_qr_payments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 public_id VARCHAR(36) NOT NULL UNIQUE,
 token_hash VARCHAR(64) NOT NULL,
 amount DECIMAL(14,2) NOT NULL,
 status VARCHAR(20) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 paid_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_local_qr_payer UNIQUE(order_id,user_id),
 CONSTRAINT fk_local_qr_order FOREIGN KEY(order_id) REFERENCES orders(id),
 CONSTRAINT fk_local_qr_user FOREIGN KEY(user_id) REFERENCES users(id)
);
