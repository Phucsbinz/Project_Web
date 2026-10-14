package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_id", nullable = false, unique = true)
  public Long orderId;

  @Column(name = "requested_by", nullable = false)
  public Long requestedBy;

  @Column(name = "reason", nullable = false, length = 2000)
  public String reason;

  @Column(name = "evidence_url", length = 1000)
  public String evidenceUrl;

  @Column(name = "evidence_public_id", length = 255)
  public String evidencePublicId;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "waive_physical_return", nullable = false)
  public boolean waivePhysicalReturn;

  @Column(name = "reviewed_by")
  public Long reviewedBy;

  @Column(name = "decision_reason", length = 1000)
  public String decisionReason;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public ReturnRequest() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getRequestedBy() {
    return requestedBy;
  }

  public String getReason() {
    return reason;
  }

  public String getEvidenceUrl() {
    return evidenceUrl;
  }

  public String getEvidencePublicId() {
    return evidencePublicId;
  }

  public String getStatus() {
    return status;
  }

  public boolean getWaivePhysicalReturn() {
    return waivePhysicalReturn;
  }

  public Long getReviewedBy() {
    return reviewedBy;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
