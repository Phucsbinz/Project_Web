package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_events")
public class DeliveryEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "assignment_id", nullable = false)
  public Long assignmentId;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "type", nullable = false, length = 30)
  public String type;

  @Column(name = "note", length = 500)
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public DeliveryEvent() {}

  public Long getId() {
    return id;
  }

  public Long getAssignmentId() {
    return assignmentId;
  }

  public Long getActorId() {
    return actorId;
  }

  public String getType() {
    return type;
  }

  public String getNote() {
    return note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
