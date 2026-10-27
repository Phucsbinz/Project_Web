package com.utetra.support;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="support_messages",uniqueConstraints=@UniqueConstraint(columnNames={"conversation_id","sender_id","client_message_id"}))
public class SupportMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="conversation_id",nullable=false,length=36) public String conversationId;
 @Column(name="sender_id",nullable=false) public Long senderId;
 @Column(name="sender_role",nullable=false,length=20) public String senderRole;
 @Column(name="client_message_id",nullable=false,length=36) public String clientMessageId;
 @Column(nullable=false,length=2000) public String body;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 public SupportMessage() {}
}
