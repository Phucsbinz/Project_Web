package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="audit_logs")
public class CatalogAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="actor_id", nullable=false) private Long actorId;
    @Column(name="action", nullable=false, length=50) private String action;
    @Column(name="entity_type", nullable=false, length=50) private String entityType;
    @Column(name="entity_id", nullable=false) private Long entityId;
    @Column(name="summary", nullable=false, length=1000) private String summary;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long value) { actorId=value; }
    public String getAction() { return action; }
    public void setAction(String value) { action=value; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { entityType=value; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long value) { entityId=value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary=value; }
    @PrePersist void created() { createdAt=Instant.now(); }
}
