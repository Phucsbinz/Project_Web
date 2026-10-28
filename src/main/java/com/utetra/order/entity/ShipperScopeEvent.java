package com.utetra.order.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="shipper_scope_events")
public class ShipperScopeEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="shipper_id") public Long shipperId;
 public Long getShipperId(){return shipperId;}
 @Column(name="store_id") public Long storeId;
 public Long getStoreId(){return storeId;}
 @Column(name="actor_id") public Long actorId;
 public Long getActorId(){return actorId;}
 @Column(name="is_active") public boolean active;
 public boolean getActive(){return active;}
 @Column(name="reason",length=500) public String reason;
 public String getReason(){return reason;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
 public Long getId(){return id;}
}
