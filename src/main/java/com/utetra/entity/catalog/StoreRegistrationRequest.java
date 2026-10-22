package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="store_registration_requests")
public class StoreRegistrationRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="store_id", nullable=false) private Long storeId;
    @Column(name="applicant_id", nullable=false) private Long applicantId;
    @Column(name="status", nullable=false, length=20) private String status;
    @Column(name="note", nullable=true, length=1000) private String note;
    @Column(name="reviewed_by", nullable=true) private Long reviewedBy;
    @Column(name="reviewed_at", nullable=true) private Instant reviewedAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Version private long version;
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long value) { storeId=value; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long value) { applicantId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public String getNote() { return note; }
    public void setNote(String value) { note=value; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long value) { reviewedBy=value; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant value) { reviewedAt=value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    @PrePersist void created() { createdAt=Instant.now(); }
}
