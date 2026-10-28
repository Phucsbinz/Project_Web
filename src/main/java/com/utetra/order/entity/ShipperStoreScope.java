package com.utetra.order.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="shipper_store_scopes",uniqueConstraints=@UniqueConstraint(columnNames={"shipper_id","store_id"}))
public class ShipperStoreScope {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public long version;
 @Column(name="shipper_id") public Long shipperId;
 public Long getShipperId(){return shipperId;}
 @Column(name="store_id") public Long storeId;
 public Long getStoreId(){return storeId;}
 @Column(name="is_active") public boolean active;
 public boolean getActive(){return active;}
 @Column(name="updated_at") public Instant updatedAt;
 public Instant getUpdatedAt(){return updatedAt;}
 public Long getId(){return id;}
}
