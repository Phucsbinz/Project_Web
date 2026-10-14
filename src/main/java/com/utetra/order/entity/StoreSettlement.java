package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "store_settlements")
public class StoreSettlement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_id", nullable = false, unique = true)
  public Long orderId;

  @Column(name = "store_id", nullable = false)
  public Long storeId;

  @Column(name = "goods_net", nullable = false, precision = 14, scale = 2)
  public BigDecimal goodsNet = BigDecimal.ZERO;

  @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
  public BigDecimal commissionRate = BigDecimal.ZERO;

  @Column(name = "commission_amount", nullable = false, precision = 14, scale = 2)
  public BigDecimal commissionAmount = BigDecimal.ZERO;

  @Column(name = "shipping_net", nullable = false, precision = 14, scale = 2)
  public BigDecimal shippingNet = BigDecimal.ZERO;

  @Column(name = "store_net", nullable = false, precision = 14, scale = 2)
  public BigDecimal storeNet = BigDecimal.ZERO;

  @Column(name = "refunded_goods", nullable = false, precision = 14, scale = 2)
  public BigDecimal refundedGoods = BigDecimal.ZERO;

  @Column(name = "refunded_shipping", nullable = false, precision = 14, scale = 2)
  public BigDecimal refundedShipping = BigDecimal.ZERO;

  @Column(name = "commission_reversed", nullable = false, precision = 14, scale = 2)
  public BigDecimal commissionReversed = BigDecimal.ZERO;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public StoreSettlement() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getStoreId() {
    return storeId;
  }

  public BigDecimal getGoodsNet() {
    return goodsNet;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public BigDecimal getCommissionAmount() {
    return commissionAmount;
  }

  public BigDecimal getShippingNet() {
    return shippingNet;
  }

  public BigDecimal getStoreNet() {
    return storeNet;
  }

  public BigDecimal getRefundedGoods() {
    return refundedGoods;
  }

  public BigDecimal getRefundedShipping() {
    return refundedShipping;
  }

  public BigDecimal getCommissionReversed() {
    return commissionReversed;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
