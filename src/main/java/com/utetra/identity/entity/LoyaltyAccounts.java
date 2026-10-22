package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="loyalty_accounts")
public class LoyaltyAccounts {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="balance",nullable=false) private Integer balance;
    @Column(name="debt",nullable=false) private Integer debt;
    @Column(name="lifetime_earned",nullable=false) private Integer lifetimeEarned;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(name="version",nullable=false) private Long version;
    protected LoyaltyAccounts() {}
    public Long getId() { return id; }
}
