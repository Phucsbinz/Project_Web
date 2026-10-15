package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="product_variants", uniqueConstraints=@UniqueConstraint(columnNames={"product_id","size_name"}))
public class ProductVariant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(name="size_name", nullable=false, length=5) private String sizeName;
    @Column(name="extra_price", nullable=false, precision=14, scale=2) private BigDecimal extraPrice;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId=value; }
    public String getSizeName() { return sizeName; }
    public void setSizeName(String value) { sizeName=value; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal value) { extraPrice=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
