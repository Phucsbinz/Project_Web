package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commission_policies")
public class CommissionPolicy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "name", nullable = false, length = 100)
  public String name;

  @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
  public BigDecimal ratePercent = BigDecimal.ZERO;

  @Column(name = "is_active", nullable = false)
  public boolean isActive;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public CommissionPolicy() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getRatePercent() {
    return ratePercent;
  }

  public boolean getIsActive() {
    return isActive;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
