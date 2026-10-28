-- Explicit Admin grants only: no shipping service scope is inferred from existing deliveries.
CREATE TABLE shipper_store_scopes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 shipper_id BIGINT NOT NULL, store_id BIGINT NOT NULL,
 is_active BOOLEAN NOT NULL, updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_shipper_store_scope UNIQUE(shipper_id,store_id),
 CONSTRAINT fk_shipper_scope_user FOREIGN KEY(shipper_id) REFERENCES users(id),
 CONSTRAINT fk_shipper_scope_store FOREIGN KEY(store_id) REFERENCES stores(id)
);
CREATE TABLE shipper_scope_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 shipper_id BIGINT NOT NULL,store_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,
 is_active BOOLEAN NOT NULL,reason VARCHAR(500) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_scope_event_shipper FOREIGN KEY(shipper_id) REFERENCES users(id),
 CONSTRAINT fk_scope_event_store FOREIGN KEY(store_id) REFERENCES stores(id),
 CONSTRAINT fk_scope_event_actor FOREIGN KEY(actor_id) REFERENCES users(id)
);
CREATE TABLE delivery_assignment_changes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 assignment_id BIGINT NOT NULL, previous_shipper_id BIGINT NULL,shipper_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,reason VARCHAR(500) NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_assignment_change_delivery FOREIGN KEY(assignment_id) REFERENCES delivery_assignments(id),
 CONSTRAINT fk_assignment_change_previous FOREIGN KEY(previous_shipper_id) REFERENCES users(id),
 CONSTRAINT fk_assignment_change_shipper FOREIGN KEY(shipper_id) REFERENCES users(id),
 CONSTRAINT fk_assignment_change_actor FOREIGN KEY(actor_id) REFERENCES users(id)
);
CREATE INDEX idx_assignment_change_history ON delivery_assignment_changes(assignment_id,created_at,id);
CREATE INDEX idx_scope_event_history ON shipper_scope_events(shipper_id,store_id,created_at,id);
-- Preserve the assignment known at migration time. Older assignments cannot be reconstructed.
INSERT INTO delivery_assignment_changes(assignment_id,previous_shipper_id,shipper_id,actor_id,reason,created_at)
 SELECT id,NULL,shipper_id,assigned_by,'Existing assignment at migration',assigned_at FROM delivery_assignments;
