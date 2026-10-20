package com.utetra.identity.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Schema mapping; native repository projections avoid exposing entities to HTTP. */
@Entity @Table(name="loyalty_transactions")
public class LoyaltyTransactions {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="account_id",nullable=false) private Long accountId;
    @Column(name="order_id",nullable=false) private Long orderId;
    @Column(name="type",nullable=false,length=20) private String type;
    @Column(name="points_delta",nullable=false) private Integer pointsDelta;
    @Column(name="balance_after",nullable=false) private Integer balanceAfter;
    @Column(name="debt_after",nullable=false) private Integer debtAfter;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected LoyaltyTransactions() {}
    public Long getId() { return id; }
}
