package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="reviews")
public class Reviews {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="author_id",nullable=false) private Long authorId;
    @Column(name="order_item_id",nullable=false) private Long orderItemId;
    @Column(name="product_id",nullable=false) private Long productId;
    @Column(name="stars",nullable=false) private Integer stars;
    @Column(name="content",nullable=true,length=2000) private String content;
    @Column(name="status",nullable=false,length=20) private String status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(name="version",nullable=false) private Long version;
    protected Reviews() {}
    public Long getId() { return id; }
}
