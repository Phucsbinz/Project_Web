package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "orders",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "checkout_key"})})
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name="group_order_id") public Long groupOrderId;
  @Column(name="group_participants",nullable=false) public int groupParticipants;
  @Column(name="group_discount",nullable=false,precision=14,scale=2) public BigDecimal groupDiscount=BigDecimal.ZERO;

  @Column(name="received_at") public Instant receivedAt;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_code", nullable = false, unique = true, length = 40)
  public String orderCode;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "store_id", nullable = false)
  public Long storeId;

  @Column(name = "delivery_area_id", nullable = false)
  public Long deliveryAreaId;

  @Column(name = "carrier_id", nullable = false)
  public Long carrierId;

  @Column(name = "commission_policy_id", nullable = false)
  public Long commissionPolicyId;

  @Column(name = "checkout_key", nullable = false, length = 36)
  public String checkoutKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  public String requestHash;

  @Column(name = "recipient_name", nullable = false, length = 100)
  public String recipientName;

  @Column(name = "recipient_phone", nullable = false, length = 20)
  public String recipientPhone;

  @Column(name = "delivery_address", nullable = false, length = 500)
  public String deliveryAddress;

  @Column(name = "store_name", nullable = false, length = 150)
  public String storeName;

  @Column(name = "delivery_area_name", nullable = false, length = 150)
  public String deliveryAreaName;

  @Column(name = "carrier_name", nullable = false, length = 150)
  public String carrierName;

  @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
  public BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "product_discount", nullable = false, precision = 14, scale = 2)
  public BigDecimal productDiscount = BigDecimal.ZERO;

  @Column(name = "points_discount", nullable = false, precision = 14, scale = 2)
  public BigDecimal pointsDiscount = BigDecimal.ZERO;

  @Column(name = "shipping_gross", nullable = false, precision = 14, scale = 2)
  public BigDecimal shippingGross = BigDecimal.ZERO;

  @Column(name = "shipping_discount", nullable = false, precision = 14, scale = 2)
  public BigDecimal shippingDiscount = BigDecimal.ZERO;

  @Column(name = "final_amount", nullable = false, precision = 14, scale = 2)
  public BigDecimal finalAmount = BigDecimal.ZERO;

  @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
  public BigDecimal commissionRate = BigDecimal.ZERO;

  @Column(name = "commission_amount", nullable = false, precision = 14, scale = 2)
  public BigDecimal commissionAmount = BigDecimal.ZERO;

  @Column(name = "store_net", nullable = false, precision = 14, scale = 2)
  public BigDecimal storeNet = BigDecimal.ZERO;

  @Column(name = "points_used", nullable = false)
  public int pointsUsed;

  @Column(name = "points_earned", nullable = false)
  public int pointsEarned;

  @Column(name = "payment_method", nullable = false, length = 20)
  public String paymentMethod;

  @Column(name = "payment_status", nullable = false, length = 20)
  public String paymentStatus;

  @Column(name = "status", nullable = false, length = 30)
  public String status;

  @Column(name = "note", length = 500)
  public String note;

  @Column(name = "cancel_reason", length = 500)
  public String cancelReason;

  @Column(name = "delivered_at")
  public Instant deliveredAt;

  @Column(name = "cancelled_at")
  public Instant cancelledAt;

  @Column(name = "paid_at")
  public Instant paidAt;

  @Column(name = "payment_confirmed_by")
  public Long paymentConfirmedBy;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public Order() {}

  public Long getId() {
    return id;
  }

  public String getOrderCode() {
    return orderCode;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getStoreId() {
    return storeId;
  }

  public Long getDeliveryAreaId() {
    return deliveryAreaId;
  }

  public Long getCarrierId() {
    return carrierId;
  }

  public Long getCommissionPolicyId() {
    return commissionPolicyId;
  }

  public String getCheckoutKey() {
    return checkoutKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public String getRecipientPhone() {
    return recipientPhone;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public String getStoreName() {
    return storeName;
  }

  public String getDeliveryAreaName() {
    return deliveryAreaName;
  }

  public String getCarrierName() {
    return carrierName;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getProductDiscount() {
    return productDiscount;
  }

  public BigDecimal getPointsDiscount() {
    return pointsDiscount;
  }

  public BigDecimal getShippingGross() {
    return shippingGross;
  }

  public BigDecimal getShippingDiscount() {
    return shippingDiscount;
  }

  public BigDecimal getFinalAmount() {
    return finalAmount;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public BigDecimal getCommissionAmount() {
    return commissionAmount;
  }

  public BigDecimal getStoreNet() {
    return storeNet;
  }

  public int getPointsUsed() {
    return pointsUsed;
  }

  public int getPointsEarned() {
    return pointsEarned;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public String getStatus() {
    return status;
  }

  public String getNote() {
    return note;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Long getPaymentConfirmedBy() {
    return paymentConfirmedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
