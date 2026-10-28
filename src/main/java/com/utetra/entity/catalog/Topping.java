package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="toppings")
public class Topping {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="image_url",length=1000) private String imageUrl;
    @Column(name="image_public_id",length=255) private String imagePublicId;
    @Column(name="name", nullable=false, length=100) private String name;
    @Column(name="price", nullable=false, precision=14, scale=2) private BigDecimal price;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false, columnDefinition="BIGINT DEFAULT 0") private long version;
    public long getVersion() { return version; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
