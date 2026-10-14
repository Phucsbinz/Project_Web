package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "provider", nullable = false, length = 20)
  public String provider;

  @Column(name = "txn_ref", nullable = false, unique = true, length = 100)
  public String txnRef;

  @Column(name = "provider_txn_id", length = 100)
  public String providerTxnId;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "paid_at")
  public Instant paidAt;

  @Column(name = "expires_at")
  public Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public PaymentTransaction() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getProvider() {
    return provider;
  }

  public String getTxnRef() {
    return txnRef;
  }

  public String getProviderTxnId() {
    return providerTxnId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getStatus() {
    return status;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
