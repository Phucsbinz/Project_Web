package com.utetra.order.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="group_orders")
public class GroupOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(name="host_user_id",nullable=false) public Long hostUserId;
 @Column(name="invite_token",nullable=false,unique=true,length=36) public String inviteToken;
 @Column(name="payment_mode",nullable=false,length=20) public String paymentMode="HOST";
 @Column(nullable=false,length=20) public String status="OPEN";
 @Column(name="expires_at",nullable=false) public Instant expiresAt;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="updated_at",nullable=false) public Instant updatedAt;
 @Column(name="order_id") public Long orderId;
}
