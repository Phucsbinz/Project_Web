package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "carts",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id"})})
public class Cart {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;

  public long getVersion() {
    return version;
  }

  @Column(name = "user_id", nullable = false, unique = true)
  public Long userId;

  @Column(name = "store_id", nullable = false)
  public Long storeId;

  @Column(name = "last_merge_key", length = 36)
  public String lastMergeKey;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  public Cart() {}

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getStoreId() {
    return storeId;
  }

  public String getLastMergeKey() {
    return lastMergeKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
