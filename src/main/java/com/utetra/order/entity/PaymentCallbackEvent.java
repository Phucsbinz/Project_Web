package com.utetra.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "payment_callback_events",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"provider", "event_key"})})
public class PaymentCallbackEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "payment_id")
  public Long paymentId;

  @Column(name = "provider", nullable = false, length = 20)
  public String provider;

  @Column(name = "event_key", nullable = false, length = 150)
  public String eventKey;

  @Column(name = "payload_hash", nullable = false, length = 64)
  public String payloadHash;

  @Column(name = "verified", nullable = false)
  public boolean verified;

  @Column(name = "outcome", nullable = false, length = 30)
  public String outcome;

  @Column(name = "received_at", nullable = false)
  public Instant receivedAt;

  @Column(name = "processed_at")
  public Instant processedAt;

  public PaymentCallbackEvent() {}

  public Long getId() {
    return id;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public String getProvider() {
    return provider;
  }

  public String getEventKey() {
    return eventKey;
  }

  public String getPayloadHash() {
    return payloadHash;
  }

  public boolean getVerified() {
    return verified;
  }

  public String getOutcome() {
    return outcome;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
