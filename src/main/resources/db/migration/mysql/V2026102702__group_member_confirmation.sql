ALTER TABLE group_order_members ADD COLUMN is_confirmed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE group_orders ADD COLUMN payment_mode VARCHAR(20) NOT NULL DEFAULT 'HOST';
ALTER TABLE group_order_members ADD COLUMN order_id BIGINT NULL;
ALTER TABLE group_order_members ADD COLUMN billed_subtotal DECIMAL(14,2) NULL;
ALTER TABLE group_order_members ADD COLUMN discount_share DECIMAL(14,2) NULL;
ALTER TABLE group_order_members ADD COLUMN shipping_share DECIMAL(14,2) NULL;
ALTER TABLE group_order_members ADD COLUMN amount_due DECIMAL(14,2) NULL;
ALTER TABLE group_order_members ADD CONSTRAINT fk_group_member_order FOREIGN KEY(order_id) REFERENCES orders(id);
