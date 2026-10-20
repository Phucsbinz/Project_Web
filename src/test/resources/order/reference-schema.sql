CREATE TABLE IF NOT EXISTS otp_challenges (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    attempts INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    reset_grant_hash VARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_addresses (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_detail VARCHAR(500) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS loyalty_accounts (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance INT NOT NULL,
    debt INT NOT NULL,
    lifetime_earned INT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS audit_logs (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    image_url VARCHAR(1000) NULL,
    image_public_id VARCHAR(255) NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS toppings (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(14,2) NOT NULL,
    image_url VARCHAR(1000) NULL,
    image_public_id VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS stores (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    commission_policy_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    description TEXT NULL,
    image_url VARCHAR(1000) NULL,
    image_public_id VARCHAR(255) NULL,
    banner_url VARCHAR(1000) NULL,
    banner_public_id VARCHAR(255) NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    accepting_orders BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS store_memberships (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    assigned_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE (user_id,store_id)
);

CREATE TABLE IF NOT EXISTS store_delivery_areas (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    area_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT NULL,
    base_price DECIMAL(14,2) NOT NULL,
    image_url VARCHAR(1000) NULL,
    image_public_id VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL,
    is_available BOOLEAN NOT NULL,
    is_featured BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS store_toppings (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    topping_id BIGINT NOT NULL,
    is_available BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE (store_id,topping_id)
);

CREATE TABLE IF NOT EXISTS store_registration_requests (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS promotions (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    scope VARCHAR(10) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    code VARCHAR(50) NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    percent DECIMAL(5,2) NOT NULL,
    max_discount DECIMAL(14,2) NOT NULL,
    min_order DECIMAL(14,2) NOT NULL,
    usage_limit INT NOT NULL,
    per_user_limit INT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    is_active BOOLEAN NOT NULL,
    funded_by VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_variants (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    size_name VARCHAR(5) NOT NULL,
    extra_price DECIMAL(14,2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE (product_id,size_name)
);

CREATE TABLE IF NOT EXISTS product_toppings (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    topping_id BIGINT NOT NULL,
    UNIQUE (product_id,topping_id)
);

CREATE TABLE IF NOT EXISTS product_favorites (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE (user_id,product_id)
);

CREATE TABLE IF NOT EXISTS product_views (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    last_viewed_at DATETIME(6) NOT NULL,
    view_count INT NOT NULL,
    UNIQUE (user_id,product_id)
);

CREATE TABLE IF NOT EXISTS promotion_products (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    UNIQUE (promotion_id,product_id)
);

CREATE TABLE IF NOT EXISTS loyalty_transactions (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    points_delta INT NOT NULL,
    balance_after INT NOT NULL,
    debt_after INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE (order_id,type)
);

CREATE TABLE IF NOT EXISTS reviews (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    stars INT NOT NULL,
    content VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_comments (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS review_media (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    secure_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(10) NOT NULL,
    display_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS comment_media (

    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    secure_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(10) NOT NULL,
    display_order INT NOT NULL
);
