ALTER TABLE carriers ADD COLUMN managed_store_id BIGINT NULL;
ALTER TABLE carriers ADD CONSTRAINT fk_carrier_managed_store FOREIGN KEY(managed_store_id) REFERENCES stores(id);
CREATE INDEX ix_carrier_managed_store ON carriers(managed_store_id);
