package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "coupon_redemptions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"promotion_id", "order_id"})})
public class CouponRedemption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "promotion_id", nullable = false)
  public Long promotionId;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "discount_snapshot", nullable = false, precision = 14, scale = 2)
  public BigDecimal discountSnapshot = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "released_at")
  public Instant releasedAt;

  @Column(name = "funded_by_snapshot", nullable = false, length = 10)
  public String fundedBySnapshot;

  @Column(name = "promotion_kind_snapshot", nullable = false, length = 20)
  public String promotionKindSnapshot;

  @Column(name = "promotion_name_snapshot", nullable = false, length = 150)
  public String promotionNameSnapshot;

  public String getFundedBySnapshot() {
    return fundedBySnapshot;
  }

  public String getPromotionKindSnapshot() {
    return promotionKindSnapshot;
  }

  public String getPromotionNameSnapshot() {
    return promotionNameSnapshot;
  }

  public CouponRedemption() {}

  public Long getId() {
    return id;
  }

  public Long getPromotionId() {
    return promotionId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getUserId() {
    return userId;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getDiscountSnapshot() {
    return discountSnapshot;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }
}
