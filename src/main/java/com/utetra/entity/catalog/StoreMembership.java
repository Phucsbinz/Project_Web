package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="store_memberships", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","store_id"}))
public class StoreMembership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="store_id", nullable=false) private Long storeId;
    @Column(name="member_role", nullable=false, length=20) private String memberRole;
    @Column(name="is_active", nullable=false) private boolean active;
    @Column(name="assigned_by", nullable=false) private Long assignedBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId=value; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String value) { memberRole=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long value) { assignedBy=value; }
    public Instant getCreatedAt() { return createdAt; }
    @PrePersist void created() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void changed() { updatedAt=Instant.now(); }
}
