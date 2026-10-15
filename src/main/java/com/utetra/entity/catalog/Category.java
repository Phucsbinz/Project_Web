package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="categories", uniqueConstraints=@UniqueConstraint(columnNames={"slug"}))
public class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="name", nullable=false, length=100) private String name;
    @Column(name="slug", nullable=false, length=100) private String slug;
    @Column(name="description", nullable=true, length=4000) private String description;
    @Column(name="image_url", nullable=true, length=1000) private String imageUrl;
    @Column(name="image_public_id", nullable=true, length=255) private String imagePublicId;
    @Column(name="display_order", nullable=false) private int displayOrder;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl=value; }
    public String getImagePublicId() { return imagePublicId; }
    public void setImagePublicId(String value) { imagePublicId=value; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int value) { displayOrder=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
