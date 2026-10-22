package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="product_toppings", uniqueConstraints=@UniqueConstraint(columnNames={"product_id","topping_id"}))
public class ProductTopping {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(name="topping_id", nullable=false) private Long toppingId;
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId=value; }
    public Long getToppingId() { return toppingId; }
    public void setToppingId(Long value) { toppingId=value; }

}
