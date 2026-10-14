package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "order_item_toppings",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"order_item_id", "topping_id"})})
public class OrderItemTopping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_item_id", nullable = false)
  public Long orderItemId;

  @Column(name = "topping_id", nullable = false)
  public Long toppingId;

  @Column(name = "topping_name", nullable = false, length = 100)
  public String toppingName;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  public BigDecimal unitPrice = BigDecimal.ZERO;

  public OrderItemTopping() {}

  public Long getId() {
    return id;
  }

  public Long getOrderItemId() {
    return orderItemId;
  }

  public Long getToppingId() {
    return toppingId;
  }

  public String getToppingName() {
    return toppingName;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
