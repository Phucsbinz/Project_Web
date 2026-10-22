package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="store_toppings", uniqueConstraints=@UniqueConstraint(columnNames={"store_id","topping_id"}))
public class StoreTopping {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="store_id", nullable=false) private Long storeId;
    @Column(name="topping_id", nullable=false) private Long toppingId;
    @Column(name="is_available", nullable=false) private boolean available;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public Long getToppingId() { return toppingId; }
    public void setToppingId(Long value) { toppingId=value; }
    public boolean getAvailable() { return available; }
    public void setAvailable(boolean value) { available=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
