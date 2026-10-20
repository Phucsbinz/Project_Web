package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="otp_challenges")
public class OtpChallenges {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=true) private Long userId;
    @Column(name="email",nullable=false,length=254) private String email;
    @Column(name="purpose",nullable=false,length=20) private String purpose;
    @Column(name="otp_hash",nullable=false,length=64) private String otpHash;
    @Column(name="attempts",nullable=false) private Integer attempts;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="consumed_at",nullable=true) private Instant consumedAt;
    @Column(name="revoked_at",nullable=true) private Instant revokedAt;
    @Column(name="reset_grant_hash",nullable=true,length=64) private String resetGrantHash;
    @Version @Column(name="version",nullable=false) private Long version;
    protected OtpChallenges() {}
    public Long getId() { return id; }
}
