package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="review_media")
public class ReviewMedia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="review_id",nullable=false) private Long reviewId;
    @Column(name="secure_url",nullable=false,length=1000) private String secureUrl;
    @Column(name="public_id",nullable=false,length=255) private String publicId;
    @Column(name="resource_type",nullable=false,length=10) private String resourceType;
    @Column(name="display_order",nullable=false) private Integer displayOrder;
    protected ReviewMedia() {}
    public Long getId() { return id; }
}
