package com.utetra.entity.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="promotion_products", uniqueConstraints=@UniqueConstraint(columnNames={"promotion_id","product_id"}))
public class PromotionProduct {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="promotion_id", nullable=false) private Long promotionId;
    @Column(name="product_id", nullable=false) private Long productId;
    public Long getId() { return id; }
    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long value) { promotionId=value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId=value; }

}
