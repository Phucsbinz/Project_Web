package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "event_key"})})
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "order_id")
  public Long orderId;

  @Column(name = "event_key", nullable = false, length = 100)
  public String eventKey;

  @Column(name = "type", nullable = false, length = 30)
  public String type;

  @Column(name = "title", nullable = false, length = 200)
  public String title;

  @Column(name = "content", nullable = false, length = 500)
  public String content;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "read_at")
  public Instant readAt;

  public Notification() {}

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getEventKey() {
    return eventKey;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getReadAt() {
    return readAt;
  }
}
