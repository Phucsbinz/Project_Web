-- Cross-module foreign keys after all owners exist. No data seeding.
ALTER TABLE store_memberships ADD CONSTRAINT fk_membership_store FOREIGN KEY(store_id) REFERENCES stores(id);
ALTER TABLE product_favorites ADD CONSTRAINT fk_favorite_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE product_views ADD CONSTRAINT fk_view_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE reviews ADD CONSTRAINT fk_review_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE reviews ADD CONSTRAINT fk_review_item FOREIGN KEY(order_item_id) REFERENCES order_items(id);
ALTER TABLE product_comments ADD CONSTRAINT fk_comment_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE product_comments ADD CONSTRAINT fk_comment_item FOREIGN KEY(order_item_id) REFERENCES order_items(id);
ALTER TABLE loyalty_transactions ADD CONSTRAINT fk_ledger_order FOREIGN KEY(order_id) REFERENCES orders(id);
-- Order references reconciled with the reviewed shared-schema proposal.


-- Order references reconciled with the reviewed shared-schema proposal.
ALTER TABLE carts ADD CONSTRAINT fk_carts_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_delivery_area_id FOREIGN KEY (delivery_area_id) REFERENCES store_delivery_areas(id);
ALTER TABLE carrier_area_rates ADD CONSTRAINT fk_carrier_area_rates_delivery_area_id FOREIGN KEY (delivery_area_id) REFERENCES store_delivery_areas(id);
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id);
ALTER TABLE coupon_redemptions ADD CONSTRAINT fk_coupon_redemptions_promotion_id FOREIGN KEY (promotion_id) REFERENCES promotions(id);
ALTER TABLE store_settlements ADD CONSTRAINT fk_store_settlements_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE cart_item_toppings ADD CONSTRAINT fk_cart_item_toppings_topping_id FOREIGN KEY (topping_id) REFERENCES toppings(id);
ALTER TABLE order_item_toppings ADD CONSTRAINT fk_order_item_toppings_topping_id FOREIGN KEY (topping_id) REFERENCES toppings(id);
