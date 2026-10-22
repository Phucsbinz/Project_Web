package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="stores", uniqueConstraints=@UniqueConstraint(columnNames={"slug"}))
public class Store {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_user_id", nullable=false) private Long ownerUserId;
    @Column(name="commission_policy_id", nullable=true) private Long commissionPolicyId;
    @Column(name="name", nullable=false, length=150) private String name;
    @Column(name="slug", nullable=false, length=150) private String slug;
    @Column(name="address", nullable=false, length=500) private String address;
    @Column(name="phone", nullable=false, length=20) private String phone;
    @Column(name="description", nullable=true, length=4000) private String description;
    @Column(name="image_url", nullable=true, length=1000) private String imageUrl;
    @Column(name="image_public_id", nullable=true, length=255) private String imagePublicId;
    @Column(name="banner_url", nullable=true, length=1000) private String bannerUrl;
    @Column(name="banner_public_id", nullable=true, length=255) private String bannerPublicId;
    // Wall-clock hours must bypass java.sql.Time's timezone/calendar conversion.
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LOCAL_TIME)
    @Column(name="open_time", nullable=false) private LocalTime openTime;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LOCAL_TIME)
    @Column(name="close_time", nullable=false) private LocalTime closeTime;
    @Column(name="status", nullable=false, length=20) private String status;
    @Column(name="accepting_orders", nullable=false) private boolean acceptingOrders;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    public Long getId() { return id; }
    public void touch() { updatedAt=Instant.now(); }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { ownerUserId=value; }
    public Long getCommissionPolicyId() { return commissionPolicyId; }
    public void setCommissionPolicyId(Long value) { commissionPolicyId=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address=value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl=value; }
    public String getImagePublicId() { return imagePublicId; }
    public void setImagePublicId(String value) { imagePublicId=value; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String value) { bannerUrl=value; }
    public String getBannerPublicId() { return bannerPublicId; }
    public void setBannerPublicId(String value) { bannerPublicId=value; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime value) { openTime=value; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime value) { closeTime=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public boolean getAcceptingOrders() { return acceptingOrders; }
    public void setAcceptingOrders(boolean value) { acceptingOrders=value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
