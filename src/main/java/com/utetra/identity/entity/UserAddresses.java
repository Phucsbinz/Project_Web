package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="user_addresses")
public class UserAddresses {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="recipient_name",nullable=false,length=100) private String recipientName;
    @Column(name="phone",nullable=false,length=20) private String phone;
    @Column(name="address_detail",nullable=false,length=500) private String addressDetail;
    @Column(name="is_default",nullable=false) private Boolean isDefault;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected UserAddresses() {}
    public Long getId() { return id; }
}
