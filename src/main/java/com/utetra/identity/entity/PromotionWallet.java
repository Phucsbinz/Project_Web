package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="promotion_wallet",uniqueConstraints={@UniqueConstraint(columnNames={"user_id","promotion_id"}),@UniqueConstraint(columnNames={"user_id","source"})})
public class PromotionWallet {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="user_id",nullable=false) public long userId;
 @Column(name="promotion_id",nullable=false) public long promotionId;
 @Column(nullable=false,length=80) public String source;
 @Column(name="granted_at",nullable=false) public Instant grantedAt;
}
