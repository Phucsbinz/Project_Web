-- Proposal only: main must allocate the version after catalog/order tables exist.
-- Do not put this file on the Flyway runtime path before integration.
ALTER TABLE store_memberships ADD CONSTRAINT fk_membership_store FOREIGN KEY(store_id) REFERENCES stores(id);
ALTER TABLE product_favorites ADD CONSTRAINT fk_favorite_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE product_views ADD CONSTRAINT fk_view_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE reviews ADD CONSTRAINT fk_review_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE reviews ADD CONSTRAINT fk_review_item FOREIGN KEY(order_item_id) REFERENCES order_items(id);
ALTER TABLE product_comments ADD CONSTRAINT fk_comment_product FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE product_comments ADD CONSTRAINT fk_comment_item FOREIGN KEY(order_item_id) REFERENCES order_items(id);
ALTER TABLE loyalty_transactions ADD CONSTRAINT fk_ledger_order FOREIGN KEY(order_id) REFERENCES orders(id);
