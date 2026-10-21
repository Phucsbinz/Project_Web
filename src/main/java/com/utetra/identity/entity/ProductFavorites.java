package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="product_favorites")
public class ProductFavorites {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="product_id",nullable=false) private Long productId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ProductFavorites() {}
    public Long getId() { return id; }
}
