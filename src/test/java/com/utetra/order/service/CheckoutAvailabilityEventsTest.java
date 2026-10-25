package com.utetra.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CheckoutAvailabilityEventsTest {
  final SimpMessagingTemplate broker=mock(SimpMessagingTemplate.class);
  final CheckoutAvailabilityEvents events=new CheckoutAvailabilityEvents(broker);
  @AfterEach void cleanup() {
    if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
  }
  @Test void rollbackDoesNotPublishAvailabilityOrConsumeAClientQuote() {
    TransactionSynchronizationManager.initSynchronization();
    events.changed(1L,List.of(10L),List.of());
    verifyNoInteractions(broker);
    TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(1));
    verifyNoInteractions(broker);
  }
  @Test void globalVoucherAndShopInventoryArePublishedAfterCommitWithoutBuyerData() {
    TransactionSynchronizationManager.initSynchronization();
    events.changed(1L,List.of(10L),List.of());events.changed(null,List.of(),List.of(7L));
    verifyNoInteractions(broker);
    TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCommit());
    var payload=org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(broker).convertAndSend(eq("/topic/stores/1/checkout"),payload.capture());
    assertThat(payload.getValue()).containsOnlyKeys("type","eventId","variantIds","promotionIds");
    verify(broker).convertAndSend(eq("/topic/checkout/promotions"),any(Object.class));
  }
  @Test void brokerFailureAfterCommitDoesNotFailTheCommittedCheckout() {
    TransactionSynchronizationManager.initSynchronization();
    doThrow(new MessageDeliveryException("Test broker down")).when(broker).convertAndSend(anyString(),any(Object.class));
    events.changed(1L,List.of(10L),List.of());
    assertThatCode(()->TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCommit())).doesNotThrowAnyException();
  }
}
