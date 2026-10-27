package com.utetra.order.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="local_qr_payments",uniqueConstraints=@UniqueConstraint(columnNames={"order_id","user_id"}))
public class LocalQrPayment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="order_id",nullable=false) public Long orderId;
 @Column(name="user_id",nullable=false) public Long userId;
 @Column(name="public_id",nullable=false,unique=true,length=36) public String publicId;
 @Column(name="token_hash",nullable=false,length=64) public String tokenHash;
 @Column(nullable=false,precision=14,scale=2) public BigDecimal amount;
 @Column(nullable=false,length=20) public String status;
 @Column(name="expires_at",nullable=false) public Instant expiresAt;
 @Column(name="paid_at") public Instant paidAt;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Version public long version;
}
