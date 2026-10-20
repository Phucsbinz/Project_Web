package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "carriers")
public class Carrier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "name", nullable = false, length = 150)
  public String name;

  @Column(name = "base_fee", nullable = false, precision = 14, scale = 2)
  public BigDecimal baseFee = BigDecimal.ZERO;

  @Column(name = "is_active", nullable = false)
  public boolean isActive;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public Carrier() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getBaseFee() {
    return baseFee;
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
