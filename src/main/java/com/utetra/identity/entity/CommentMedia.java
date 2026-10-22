package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="comment_media")
public class CommentMedia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="comment_id",nullable=false) private Long commentId;
    @Column(name="secure_url",nullable=false,length=1000) private String secureUrl;
    @Column(name="public_id",nullable=false,length=255) private String publicId;
    @Column(name="resource_type",nullable=false,length=10) private String resourceType;
    @Column(name="display_order",nullable=false) private Integer displayOrder;
    protected CommentMedia() {}
    public Long getId() { return id; }
}
