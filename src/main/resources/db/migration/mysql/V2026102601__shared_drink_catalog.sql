-- Shared menu identity is explicit; existing products remain separate when no key is assigned.
ALTER TABLE products ADD COLUMN catalog_key VARCHAR(150) NULL;
CREATE UNIQUE INDEX uq_products_store_catalog_key ON products(store_id,catalog_key);
