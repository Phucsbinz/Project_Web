package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "order_status_history",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"order_id", "order_version"})})
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "from_status", length = 30)
  public String fromStatus;

  @Column(name = "to_status", nullable = false, length = 30)
  public String toStatus;

  @Column(name = "event_type", nullable = false, length = 30)
  public String eventType;

  @Column(name = "changed_by")
  public Long changedBy;

  @Column(name = "reason", length = 500)
  public String reason;

  @Column(name = "order_version", nullable = false)
  public Long orderVersion;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public OrderStatusHistory() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public String getEventType() {
    return eventType;
  }

  public Long getChangedBy() {
    return changedBy;
  }

  public String getReason() {
    return reason;
  }

  public Long getOrderVersion() {
    return orderVersion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
