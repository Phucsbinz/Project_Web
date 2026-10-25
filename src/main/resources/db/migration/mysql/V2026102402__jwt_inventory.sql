ALTER TABLE auth_sessions ADD COLUMN access_token_id VARCHAR(36) NULL;
ALTER TABLE auth_sessions ADD COLUMN access_token_hash VARCHAR(64) NULL;
ALTER TABLE auth_sessions ADD COLUMN access_expires_at TIMESTAMP(6) NULL;
ALTER TABLE product_variants ADD COLUMN stock_quantity INT NULL;
ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD CONSTRAINT ck_variant_stock CHECK(stock_quantity IS NULL OR stock_quantity>=0);
ALTER TABLE order_items ADD COLUMN stock_reserved BOOLEAN NOT NULL DEFAULT FALSE;
