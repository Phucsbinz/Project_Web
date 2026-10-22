-- Integrated catalog schema; no sample data or accounts.

CREATE TABLE stores (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_user_id BIGINT NOT NULL,
 commission_policy_id BIGINT NULL,
 name VARCHAR(150) NOT NULL,
 slug VARCHAR(150) NOT NULL,
 address VARCHAR(500) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 description VARCHAR(4000) NULL,
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
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_stores UNIQUE (slug),
 CONSTRAINT ck_stores_0 CHECK (open_time < close_time),
 CONSTRAINT ck_stores_1 CHECK (status IN ('PENDING_APPROVAL','ACTIVE','REJECTED','SUSPENDED')),
 CONSTRAINT ck_stores_2 CHECK (status <> 'ACTIVE' OR commission_policy_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 slug VARCHAR(100) NOT NULL,
 description VARCHAR(4000) NULL,
 image_url VARCHAR(1000) NULL,
 image_public_id VARCHAR(255) NULL,
 display_order INT NOT NULL,
 is_active BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_categories UNIQUE (slug),
 CONSTRAINT ck_categories_0 CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 name VARCHAR(150) NOT NULL,
 slug VARCHAR(150) NOT NULL,
 description VARCHAR(4000) NULL,
 base_price DECIMAL(14,2) NOT NULL,
 image_url VARCHAR(1000) NULL,
 image_public_id VARCHAR(255) NULL,
 is_active BOOLEAN NOT NULL,
 is_available BOOLEAN NOT NULL,
 is_featured BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_products UNIQUE (slug),
 CONSTRAINT ck_products_0 CHECK (base_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_variants (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 size_name VARCHAR(5) NOT NULL,
 extra_price DECIMAL(14,2) NOT NULL,
 is_active BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_product_variants UNIQUE (product_id,size_name),
 CONSTRAINT ck_product_variants_0 CHECK (size_name IN ('S','M','L')),
 CONSTRAINT ck_product_variants_1 CHECK (extra_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE toppings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 price DECIMAL(14,2) NOT NULL,
 is_active BOOLEAN NOT NULL,
 image_url VARCHAR(1000) NULL,
 image_public_id VARCHAR(255) NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT ck_toppings_0 CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE store_toppings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 topping_id BIGINT NOT NULL,
 is_available BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_store_toppings UNIQUE (store_id,topping_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_toppings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 topping_id BIGINT NOT NULL,
 CONSTRAINT uq_product_toppings UNIQUE (product_id,topping_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE store_delivery_areas (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 area_code VARCHAR(50) NOT NULL,
 name VARCHAR(150) NOT NULL,
 is_active BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_store_delivery_areas UNIQUE (store_id,area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE store_registration_requests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 applicant_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL,
 note VARCHAR(1000) NULL,
 reviewed_by BIGINT NULL,
 reviewed_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_store_registration_requests_0 CHECK (status IN ('PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE promotions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NULL,
 created_by BIGINT NOT NULL,
 scope VARCHAR(10) NOT NULL,
 kind VARCHAR(20) NOT NULL,
 code VARCHAR(50) NULL,
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
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_promotions UNIQUE (code),
 CONSTRAINT ck_promotions_0 CHECK (scope IN ('GLOBAL','SHOP')),
 CONSTRAINT ck_promotions_1 CHECK (kind IN ('PRODUCT_PERCENT','SHIPPING_PERCENT')),
 CONSTRAINT ck_promotions_2 CHECK (percent > 0 AND percent <= 100),
 CONSTRAINT ck_promotions_3 CHECK (max_discount >= 0 AND min_order >= 0),
 CONSTRAINT ck_promotions_4 CHECK (usage_limit >= per_user_limit AND per_user_limit > 0),
 CONSTRAINT ck_promotions_5 CHECK (starts_at < ends_at),
 CONSTRAINT ck_promotions_6 CHECK ((scope = 'GLOBAL' AND store_id IS NULL AND funded_by = 'APP') OR (scope = 'SHOP' AND store_id IS NOT NULL AND funded_by = 'SHOP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE promotion_products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 promotion_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 CONSTRAINT uq_promotion_products UNIQUE (promotion_id,product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE stores ADD CONSTRAINT fk_stores_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id);
ALTER TABLE stores ADD CONSTRAINT fk_stores_commission_policy_id FOREIGN KEY (commission_policy_id) REFERENCES commission_policies(id);
ALTER TABLE products ADD CONSTRAINT fk_products_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE products ADD CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE product_variants ADD CONSTRAINT fk_product_variants_product_id FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE store_toppings ADD CONSTRAINT fk_store_toppings_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE store_toppings ADD CONSTRAINT fk_store_toppings_topping_id FOREIGN KEY (topping_id) REFERENCES toppings(id);
ALTER TABLE product_toppings ADD CONSTRAINT fk_product_toppings_product_id FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE product_toppings ADD CONSTRAINT fk_product_toppings_topping_id FOREIGN KEY (topping_id) REFERENCES toppings(id);
ALTER TABLE store_delivery_areas ADD CONSTRAINT fk_store_delivery_areas_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE store_registration_requests ADD CONSTRAINT fk_store_registration_requests_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE store_registration_requests ADD CONSTRAINT fk_store_registration_requests_applicant_id FOREIGN KEY (applicant_id) REFERENCES users(id);
ALTER TABLE store_registration_requests ADD CONSTRAINT fk_store_registration_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id);
ALTER TABLE promotions ADD CONSTRAINT fk_promotions_store_id FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE promotions ADD CONSTRAINT fk_promotions_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE promotion_products ADD CONSTRAINT fk_promotion_products_promotion_id FOREIGN KEY (promotion_id) REFERENCES promotions(id);
ALTER TABLE promotion_products ADD CONSTRAINT fk_promotion_products_product_id FOREIGN KEY (product_id) REFERENCES products(id);
