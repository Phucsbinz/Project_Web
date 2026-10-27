-- Private customer-to-staff conversations. No staff-to-staff or public chat channel.
CREATE TABLE support_conversations (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 customer_id BIGINT NOT NULL,
 scope_key VARCHAR(40) NOT NULL,
 channel VARCHAR(10) NOT NULL,
 store_id BIGINT NULL,
 last_message_id BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_support_customer_scope UNIQUE(customer_id,scope_key),
 CONSTRAINT fk_support_customer FOREIGN KEY(customer_id) REFERENCES users(id),
 CONSTRAINT fk_support_store FOREIGN KEY(store_id) REFERENCES stores(id),
 INDEX ix_support_scope_updated(channel,store_id,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE support_messages (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 conversation_id VARCHAR(36) NOT NULL,
 sender_id BIGINT NOT NULL,
 sender_role VARCHAR(20) NOT NULL,
 client_message_id VARCHAR(36) NOT NULL,
 body VARCHAR(2000) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_support_retry UNIQUE(conversation_id,sender_id,client_message_id),
 CONSTRAINT fk_support_message_thread FOREIGN KEY(conversation_id) REFERENCES support_conversations(id),
 CONSTRAINT fk_support_sender FOREIGN KEY(sender_id) REFERENCES users(id),
 INDEX ix_support_messages(conversation_id,id),
 INDEX ix_support_sender_time(sender_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
