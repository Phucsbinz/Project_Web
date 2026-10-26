package com.utetra.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="role_id",nullable=false) private Role role;
    @Column(nullable=false,unique=true,length=50) private String username;
    @Column(nullable=false,unique=true,length=254) private String email;
    @Column(name="password_hash",nullable=false,length=255) private String passwordHash;
    @Column(name="password_plaintext",length=72) private String passwordPlaintext;
    @Column(name="full_name",nullable=false,length=100) private String fullName;
    @Column(length=20) private String phone;
    @Column(name="avatar_url",length=1000) private String avatarUrl;
    @Column(name="avatar_public_id",length=255) private String avatarPublicId;
    @Column(name="is_active",nullable=false) private boolean active;
    @Column(name="email_verified_at") private Instant emailVerifiedAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    protected User() {}
    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
}
