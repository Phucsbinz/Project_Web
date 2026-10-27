package com.utetra.support;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="support_conversations",uniqueConstraints=@UniqueConstraint(columnNames={"customer_id","scope_key"}))
public class SupportConversation {
 @Id @Column(length=36) public String id;
 @Column(name="customer_id",nullable=false) public Long customerId;
 @Column(name="scope_key",nullable=false,length=40) public String scopeKey;
 @Column(nullable=false,length=10) public String channel;
 @Column(name="store_id") public Long storeId;
 @Column(name="last_message_id",nullable=false) public long lastMessageId;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="updated_at",nullable=false) public Instant updatedAt;
 public SupportConversation() {}
}
