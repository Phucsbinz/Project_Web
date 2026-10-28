CREATE TABLE promotion_wallet (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 promotion_id BIGINT NOT NULL,
 source VARCHAR(80) NOT NULL,
 granted_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_wallet_promotion UNIQUE(user_id,promotion_id),
 CONSTRAINT uq_wallet_source UNIQUE(user_id,source),
 CONSTRAINT fk_wallet_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_wallet_promotion FOREIGN KEY(promotion_id) REFERENCES promotions(id),
 INDEX ix_wallet_promotion(promotion_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
