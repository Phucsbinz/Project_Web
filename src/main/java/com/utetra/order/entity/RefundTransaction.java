package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refund_transactions")
public class RefundTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "payment_id", nullable = false)
  public Long paymentId;

  @Column(name = "return_request_id")
  public Long returnRequestId;

  @Column(name = "refund_ref", nullable = false, unique = true, length = 100)
  public String refundRef;

  @Column(name = "provider_ref", length = 100)
  public String providerRef;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "reason", nullable = false, length = 1000)
  public String reason;

  @Column(name = "evidence_ref", length = 500)
  public String evidenceRef;

  @Column(name = "approved_by", nullable = false)
  public Long approvedBy;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "completed_at")
  public Instant completedAt;

  public RefundTransaction() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public Long getReturnRequestId() {
    return returnRequestId;
  }

  public String getRefundRef() {
    return refundRef;
  }

  public String getProviderRef() {
    return providerRef;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public String getEvidenceRef() {
    return evidenceRef;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
