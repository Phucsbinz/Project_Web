-- Catalog-owned additions already present in integration/utetra migrations.
-- Review-only proposal for the standalone branch, NOT an automatically applied migration.
-- Prerequisites: catalog tables, reviewed baseline and a backup.
-- Do not run against integration's DB: these columns/indexes may already exist.
ALTER TABLE product_variants ADD COLUMN stock_quantity INT NULL;
ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD CONSTRAINT ck_variant_stock CHECK(stock_quantity IS NULL OR stock_quantity>=0);
ALTER TABLE products ADD COLUMN catalog_key VARCHAR(150) NULL;
CREATE UNIQUE INDEX uq_products_store_catalog_key ON products(store_id,catalog_key);
