package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="promotions", uniqueConstraints=@UniqueConstraint(columnNames={"code"}))
public class Promotion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="store_id", nullable=true) private Long storeId;
    @Column(name="created_by", nullable=false) private Long createdBy;
    @Column(name="scope", nullable=false, length=10) private String scope;
    @Column(name="kind", nullable=false, length=20) private String kind;
    @Column(name="code", nullable=true, length=50) private String code;
    @Column(name="name", nullable=false, length=150) private String name;
    @Column(name="percent", nullable=false, precision=5, scale=2) private BigDecimal percent;
    @Column(name="max_discount", nullable=false, precision=14, scale=2) private BigDecimal maxDiscount;
    @Column(name="min_order", nullable=false, precision=14, scale=2) private BigDecimal minOrder;
    @Column(name="usage_limit", nullable=false) private int usageLimit;
    @Column(name="per_user_limit", nullable=false) private int perUserLimit;
    @Column(name="starts_at", nullable=false) private Instant startsAt;
    @Column(name="ends_at", nullable=false) private Instant endsAt;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="funded_by", nullable=false, length=10) private String fundedBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    public Long getId() { return id; }
    public void touch() { updatedAt=Instant.now(); }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long value) { createdBy=value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope=value; }
    public String getKind() { return kind; }
    public void setKind(String value) { kind=value; }
    public String getCode() { return code; }
    public void setCode(String value) { code=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal value) { percent=value; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal value) { maxDiscount=value; }
    public BigDecimal getMinOrder() { return minOrder; }
    public void setMinOrder(BigDecimal value) { minOrder=value; }
    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int value) { usageLimit=value; }
    public int getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(int value) { perUserLimit=value; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant value) { startsAt=value; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant value) { endsAt=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public String getFundedBy() { return fundedBy; }
    public void setFundedBy(String value) { fundedBy=value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
