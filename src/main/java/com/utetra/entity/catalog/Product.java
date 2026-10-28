package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="products", uniqueConstraints=@UniqueConstraint(columnNames={"slug"}))
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="store_id", nullable=false) private Long storeId;
    @Column(name="category_id", nullable=false) private Long categoryId;
    @Column(name="name", nullable=false, length=150) private String name;
    @Column(name="slug", nullable=false, length=150) private String slug;
    @Column(name="catalog_key", length=150) private String catalogKey;
    public String getCatalogKey() { return catalogKey; }
    public void setCatalogKey(String value) { catalogKey=value; }
    @Column(name="description", nullable=true, length=4000) private String description;
    @Column(name="base_price", nullable=false, precision=14, scale=2) private BigDecimal basePrice;
    @Column(name="image_url", nullable=true, length=1000) private String imageUrl;
    @Column(name="image_public_id", nullable=true, length=255) private String imagePublicId;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="is_available", nullable=false) private boolean available;
    @Column(name="is_featured", nullable=false) private boolean featured;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    public Long getId() { return id; }
    public void touch() { updatedAt=Instant.now(); }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal value) { basePrice=value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl=value; }
    public String getImagePublicId() { return imagePublicId; }
    public void setImagePublicId(String value) { imagePublicId=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public boolean getAvailable() { return available; }
    public void setAvailable(boolean value) { available=value; }
    public boolean getFeatured() { return featured; }
    public void setFeatured(boolean value) { featured=value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
