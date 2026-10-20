package com.utetra.order.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "cart_item_toppings",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"cart_item_id", "topping_id"})})
public class CartItemTopping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "cart_item_id", nullable = false)
  public Long cartItemId;

  @Column(name = "topping_id", nullable = false)
  public Long toppingId;

  public CartItemTopping() {}

  public Long getId() {
    return id;
  }

  public Long getCartItemId() {
    return cartItemId;
  }

  public Long getToppingId() {
    return toppingId;
  }
}
