package com.utetra.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.entity.Order;
import com.utetra.order.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OrderEventDeliveryTest {
  final OrderDataRepository data = mock(OrderDataRepository.class);
  final CommerceReferenceRepository refs = mock(CommerceReferenceRepository.class);
  final SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
  final OrderSupport support =
      new OrderSupport(data, refs, broker, Clock.systemUTC(), new ObjectMapper());
  final Order order = new Order();

  @BeforeEach
  void fixture() {
    order.id = 1L;
    order.userId = 1L;
    order.storeId = 1L;
    order.orderCode = "TEST-ORDER";
    order.status = "NEW";
    order.version = 0;
    when(refs.recipients(1)).thenReturn(List.of(2L));
  }

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void eventRequiresATransactionBeforeWritingAnything() {
    assertThatThrownBy(() -> support.event(order, new Actor(1, "ROLE_USER"), null, "CREATED", ""))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(data, broker);
  }

  @Test
  void noRealtimeMessageIsSentBeforeCommitOrWhenRolledBack() {
    TransactionSynchronizationManager.initSynchronization();
    support.event(order, new Actor(1, "ROLE_USER"), null, "CREATED", "");
    verifyNoInteractions(broker);
    for (var sync : TransactionSynchronizationManager.getSynchronizations())
      sync.afterCompletion(1);
    verifyNoInteractions(broker);
  }

  @Test
  void brokerFailureAfterCommitDoesNotReportTheCommittedPurchaseAsFailed() {
    TransactionSynchronizationManager.initSynchronization();
    doThrow(new MessageDeliveryException("Test broker outage"))
        .when(broker)
        .convertAndSendToUser(anyString(), eq("/queue/orders"), any(Object.class));
    support.event(order, new Actor(1, "ROLE_USER"), null, "CREATED", "");
    for (var sync : TransactionSynchronizationManager.getSynchronizations())
      assertThatCode(sync::afterCommit).doesNotThrowAnyException();
    verify(data, times(3)).save(any()); // history and two durable recipient notifications
  }
}
