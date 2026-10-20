package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="product_views")
public class ProductViews {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="product_id",nullable=false) private Long productId;
    @Column(name="last_viewed_at",nullable=false) private Instant lastViewedAt;
    @Column(name="view_count",nullable=false) private Integer viewCount;
    protected ProductViews() {}
    public Long getId() { return id; }
}
