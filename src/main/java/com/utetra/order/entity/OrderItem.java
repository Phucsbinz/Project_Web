package com.utetra.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Column(name="stock_reserved",nullable=false,columnDefinition="boolean default false")
  public boolean stockReserved;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "variant_id", nullable = false)
  public Long variantId;

  @Column(name = "product_name", nullable = false, length = 150)
  public String productName;

  @Column(name = "size_name", nullable = false, length = 5)
  public String sizeName;

  @Column(name = "base_price", nullable = false, precision = 14, scale = 2)
  public BigDecimal basePrice = BigDecimal.ZERO;

  @Column(name = "extra_price", nullable = false, precision = 14, scale = 2)
  public BigDecimal extraPrice = BigDecimal.ZERO;

  @Column(name = "drink_unit_price", nullable = false, precision = 14, scale = 2)
  public BigDecimal drinkUnitPrice = BigDecimal.ZERO;

  @Column(name = "topping_unit_total", nullable = false, precision = 14, scale = 2)
  public BigDecimal toppingUnitTotal = BigDecimal.ZERO;

  @Column(name = "quantity", nullable = false)
  public int quantity;

  @Column(name = "sugar_level", nullable = false)
  public int sugarLevel;

  @Column(name = "ice_level", nullable = false)
  public int iceLevel;

  @Column(name = "note", nullable = false, length = 255)
  public String note;

  @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
  public BigDecimal lineTotal = BigDecimal.ZERO;

  @Column(name = "discount_allocated", nullable = false, precision = 14, scale = 2)
  public BigDecimal discountAllocated = BigDecimal.ZERO;

  public OrderItem() {}

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getVariantId() {
    return variantId;
  }

  public String getProductName() {
    return productName;
  }

  public String getSizeName() {
    return sizeName;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public BigDecimal getExtraPrice() {
    return extraPrice;
  }

  public BigDecimal getDrinkUnitPrice() {
    return drinkUnitPrice;
  }

  public BigDecimal getToppingUnitTotal() {
    return toppingUnitTotal;
  }

  public int getQuantity() {
    return quantity;
  }

  public int getSugarLevel() {
    return sugarLevel;
  }

  public int getIceLevel() {
    return iceLevel;
  }

  public String getNote() {
    return note;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }

  public BigDecimal getDiscountAllocated() {
    return discountAllocated;
  }
}
