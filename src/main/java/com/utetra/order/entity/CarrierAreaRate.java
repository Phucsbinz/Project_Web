package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "carrier_area_rates",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"carrier_id", "delivery_area_id"})})
public class CarrierAreaRate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "carrier_id", nullable = false)
  public Long carrierId;

  @Column(name = "delivery_area_id", nullable = false)
  public Long deliveryAreaId;

  @Column(name = "additional_fee", nullable = false, precision = 14, scale = 2)
  public BigDecimal additionalFee = BigDecimal.ZERO;

  @Column(name = "is_active", nullable = false)
  public boolean isActive;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public CarrierAreaRate() {}

  public Long getId() {
    return id;
  }

  public Long getCarrierId() {
    return carrierId;
  }

  public Long getDeliveryAreaId() {
    return deliveryAreaId;
  }

  public BigDecimal getAdditionalFee() {
    return additionalFee;
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
