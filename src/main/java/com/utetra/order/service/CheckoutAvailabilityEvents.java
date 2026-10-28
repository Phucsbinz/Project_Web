package com.utetra.order.service;

import java.util.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** Public availability signals only. No buyer, bank reference, order or address data. */
@Component
public class CheckoutAvailabilityEvents {
  private final SimpMessagingTemplate broker;
  public CheckoutAvailabilityEvents(SimpMessagingTemplate broker) { this.broker=broker; }
  public void changed(Long store,Collection<Long> variants,Collection<Long> promotions) {
    if(!TransactionSynchronizationManager.isSynchronizationActive())
      throw new IllegalStateException("Availability events require a transaction");
    String destination=store==null?"/topic/checkout/promotions":"/topic/stores/"+store+"/checkout";
    Map<String,Object> event=Map.of("type","AVAILABILITY_CHANGED","eventId",UUID.randomUUID().toString(),
        "variantIds",List.copyOf(variants),"promotionIds",List.copyOf(promotions));
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() {
        try { broker.convertAndSend(destination,event); }
        catch(org.springframework.messaging.MessagingException e) {
          org.slf4j.LoggerFactory.getLogger(CheckoutAvailabilityEvents.class).warn("Checkout availability signal unavailable; clients can poll.");
        }
      }
    });
  }
}
