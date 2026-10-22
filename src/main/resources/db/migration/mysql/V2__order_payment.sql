-- Order-payment owned tables only. External FK additions require shared-schema review.

CREATE TABLE commission_policies (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rate_percent DECIMAL(5,2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE carriers (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    base_fee DECIMAL(14,2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE carts (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_carts_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    store_id BIGINT NOT NULL,
    last_merge_key VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE orders (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    store_id BIGINT NOT NULL,
    delivery_area_id BIGINT NOT NULL,
    carrier_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_carrier_id FOREIGN KEY (carrier_id) REFERENCES carriers(id),
    commission_policy_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_commission_policy_id FOREIGN KEY (commission_policy_id) REFERENCES commission_policies(id),
    checkout_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    store_name VARCHAR(150) NOT NULL,
    delivery_area_name VARCHAR(150) NOT NULL,
    carrier_name VARCHAR(150) NOT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    product_discount DECIMAL(14,2) NOT NULL,
    points_discount DECIMAL(14,2) NOT NULL,
    shipping_gross DECIMAL(14,2) NOT NULL,
    shipping_discount DECIMAL(14,2) NOT NULL,
    final_amount DECIMAL(14,2) NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL,
    commission_amount DECIMAL(14,2) NOT NULL,
    store_net DECIMAL(14,2) NOT NULL,
    points_used INT NOT NULL,
    points_earned INT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    delivered_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    paid_at DATETIME(6) NULL,
    payment_confirmed_by BIGINT NULL,
    CONSTRAINT fk_orders_payment_confirmed_by FOREIGN KEY (payment_confirmed_by) REFERENCES users(id),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id,checkout_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE carrier_area_rates (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    carrier_id BIGINT NOT NULL,
    CONSTRAINT fk_carrier_area_rates_carrier_id FOREIGN KEY (carrier_id) REFERENCES carriers(id),
    delivery_area_id BIGINT NOT NULL,
    additional_fee DECIMAL(14,2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE (carrier_id,delivery_area_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE cart_items (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_items_cart_id FOREIGN KEY (cart_id) REFERENCES carts(id),
    variant_id BIGINT NOT NULL,
    sugar_level INT NOT NULL,
    ice_level INT NOT NULL,
    quantity INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    configuration_hash VARCHAR(64) NOT NULL,
    UNIQUE (cart_id,configuration_hash)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_items (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    size_name VARCHAR(5) NOT NULL,
    base_price DECIMAL(14,2) NOT NULL,
    extra_price DECIMAL(14,2) NOT NULL,
    drink_unit_price DECIMAL(14,2) NOT NULL,
    topping_unit_total DECIMAL(14,2) NOT NULL,
    quantity INT NOT NULL,
    sugar_level INT NOT NULL,
    ice_level INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    line_total DECIMAL(14,2) NOT NULL,
    discount_allocated DECIMAL(14,2) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_status_history (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_status_history_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    changed_by BIGINT NULL,
    CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id),
    reason VARCHAR(500) NULL,
    order_version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE (order_id,order_version)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE notifications (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    order_id BIGINT NULL,
    CONSTRAINT fk_notifications_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    event_key VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    UNIQUE (user_id,event_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE coupon_redemptions (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_coupon_redemptions_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_coupon_redemptions_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    discount_snapshot DECIMAL(14,2) NOT NULL,
    funded_by_snapshot VARCHAR(10) NOT NULL,
    promotion_kind_snapshot VARCHAR(20) NOT NULL,
    promotion_name_snapshot VARCHAR(150) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    released_at DATETIME(6) NULL,
    UNIQUE (promotion_id,order_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE payment_transactions (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_transactions_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    provider VARCHAR(20) NOT NULL,
    txn_ref VARCHAR(100) NOT NULL UNIQUE,
    provider_txn_id VARCHAR(100) NULL,
    amount DECIMAL(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE return_requests (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_return_requests_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    requested_by BIGINT NOT NULL,
    CONSTRAINT fk_return_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users(id),
    reason VARCHAR(2000) NOT NULL,
    evidence_url VARCHAR(1000) NULL,
    evidence_public_id VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    waive_physical_return BOOLEAN NOT NULL,
    reviewed_by BIGINT NULL,
    CONSTRAINT fk_return_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id),
    decision_reason VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE store_settlements (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_store_settlements_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    store_id BIGINT NOT NULL,
    goods_net DECIMAL(14,2) NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL,
    commission_amount DECIMAL(14,2) NOT NULL,
    shipping_net DECIMAL(14,2) NOT NULL,
    store_net DECIMAL(14,2) NOT NULL,
    refunded_goods DECIMAL(14,2) NOT NULL,
    refunded_shipping DECIMAL(14,2) NOT NULL,
    commission_reversed DECIMAL(14,2) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE delivery_assignments (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_delivery_assignments_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    shipper_id BIGINT NOT NULL,
    CONSTRAINT fk_delivery_assignments_shipper_id FOREIGN KEY (shipper_id) REFERENCES users(id),
    assigned_by BIGINT NOT NULL,
    CONSTRAINT fk_delivery_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE cart_item_toppings (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_item_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_item_toppings_cart_item_id FOREIGN KEY (cart_item_id) REFERENCES cart_items(id),
    topping_id BIGINT NOT NULL,
    UNIQUE (cart_item_id,topping_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_item_toppings (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    CONSTRAINT fk_order_item_toppings_order_item_id FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    topping_id BIGINT NOT NULL,
    topping_name VARCHAR(100) NOT NULL,
    unit_price DECIMAL(14,2) NOT NULL,
    UNIQUE (order_item_id,topping_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE payment_callback_events (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NULL,
    CONSTRAINT fk_payment_callback_events_payment_id FOREIGN KEY (payment_id) REFERENCES payment_transactions(id),
    provider VARCHAR(20) NOT NULL,
    event_key VARCHAR(150) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    verified BOOLEAN NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    UNIQUE (provider,event_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE refund_transactions (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_refund_transactions_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    payment_id BIGINT NOT NULL,
    CONSTRAINT fk_refund_transactions_payment_id FOREIGN KEY (payment_id) REFERENCES payment_transactions(id),
    return_request_id BIGINT NULL,
    CONSTRAINT fk_refund_transactions_return_request_id FOREIGN KEY (return_request_id) REFERENCES return_requests(id),
    refund_ref VARCHAR(100) NOT NULL UNIQUE,
    provider_ref VARCHAR(100) NULL,
    amount DECIMAL(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    evidence_ref VARCHAR(500) NULL,
    approved_by BIGINT NOT NULL,
    CONSTRAINT fk_refund_transactions_approved_by FOREIGN KEY (approved_by) REFERENCES users(id),
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE delivery_events (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    CONSTRAINT fk_delivery_events_assignment_id FOREIGN KEY (assignment_id) REFERENCES delivery_assignments(id),
    actor_id BIGINT NOT NULL,
    CONSTRAINT fk_delivery_events_actor_id FOREIGN KEY (actor_id) REFERENCES users(id),
    type VARCHAR(30) NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
