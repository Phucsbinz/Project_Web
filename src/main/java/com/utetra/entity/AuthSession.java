package com.utetra.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="auth_sessions")
public class AuthSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="refresh_token_hash",nullable=false,unique=true,length=64) private String refreshTokenHash;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="last_refreshed_at") private Instant lastRefreshedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @Version private long version;
    protected AuthSession() {}
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
