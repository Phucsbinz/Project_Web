package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="store_delivery_areas", uniqueConstraints=@UniqueConstraint(columnNames={"store_id","area_code"}))
public class StoreDeliveryArea {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="store_id", nullable=false) private Long storeId;
    @Column(name="area_code", nullable=false, length=50) private String areaCode;
    @Column(name="name", nullable=false, length=150) private String name;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String value) { areaCode=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
