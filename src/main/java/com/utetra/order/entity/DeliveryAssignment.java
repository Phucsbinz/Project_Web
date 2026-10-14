package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "order_id", nullable = false, unique = true)
  public Long orderId;

  @Column(name = "shipper_id", nullable = false)
  public Long shipperId;

  @Column(name = "assigned_by", nullable = false)
  public Long assignedBy;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "assigned_at", nullable = false)
  public Instant assignedAt;

  @Column(name = "completed_at")
  public Instant completedAt;

  public DeliveryAssignment() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getShipperId() {
    return shipperId;
  }

  public Long getAssignedBy() {
    return assignedBy;
  }

  public String getStatus() {
    return status;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
