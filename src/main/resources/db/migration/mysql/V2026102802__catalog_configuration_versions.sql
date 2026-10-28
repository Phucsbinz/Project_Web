-- Expected-version support for stale category, topping and delivery-area forms.
ALTER TABLE categories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE toppings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE store_delivery_areas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
