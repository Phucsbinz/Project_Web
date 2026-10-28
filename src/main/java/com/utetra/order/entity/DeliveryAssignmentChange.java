package com.utetra.order.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="delivery_assignment_changes")
public class DeliveryAssignmentChange {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="assignment_id") public Long assignmentId;
 public Long getAssignmentId(){return assignmentId;}
 @Column(name="previous_shipper_id") public Long previousShipperId;
 public Long getPreviousShipperId(){return previousShipperId;}
 @Column(name="shipper_id") public Long shipperId;
 public Long getShipperId(){return shipperId;}
 @Column(name="actor_id") public Long actorId;
 public Long getActorId(){return actorId;}
 @Column(name="reason",length=500) public String reason;
 public String getReason(){return reason;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
 public Long getId(){return id;}
}
