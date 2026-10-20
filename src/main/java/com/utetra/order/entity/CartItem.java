package com.utetra.order.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"cart_id", "configuration_hash"})})
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "cart_id", nullable = false)
  public Long cartId;

  @Column(name = "variant_id", nullable = false)
  public Long variantId;

  @Column(name = "sugar_level", nullable = false)
  public int sugarLevel;

  @Column(name = "ice_level", nullable = false)
  public int iceLevel;

  @Column(name = "quantity", nullable = false)
  public int quantity;

  @Column(name = "note", nullable = false, length = 255)
  public String note;

  @Column(name = "configuration_hash", nullable = false, length = 64)
  public String configurationHash;

  public CartItem() {}

  public Long getId() {
    return id;
  }

  public Long getCartId() {
    return cartId;
  }

  public Long getVariantId() {
    return variantId;
  }

  public int getSugarLevel() {
    return sugarLevel;
  }

  public int getIceLevel() {
    return iceLevel;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getNote() {
    return note;
  }

  public String getConfigurationHash() {
    return configurationHash;
  }
}
