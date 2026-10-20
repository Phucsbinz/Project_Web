package com.utetra.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.utetra.security.UtetraPrincipal;
import com.utetra.service.SessionIdentityService;
import com.utetra.service.StoreAccessPolicy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

class OrderSocketGuardTest {
  private static class InspectableTransport extends WebSocketTransportRegistration {
    java.util.List<org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory>
        factories() {
      return getDecoratorFactories();
    }
  }

  @Test
  void revokedSessionIsClosedEvenWithoutFurtherClientMessages() throws Exception {
    SessionIdentityService identity = mock(SessionIdentityService.class);
    StoreAccessPolicy stores = mock(StoreAccessPolicy.class);
    OrderSocketGuard guard = new OrderSocketGuard(identity, stores);
    WebSocketHandler delegate = mock(WebSocketHandler.class);
    WebSocketSession session = mock(WebSocketSession.class);
    var principal = new UtetraPrincipal(1L, 1L, "ROLE_USER", Instant.now().plusSeconds(300));
    when(session.getId()).thenReturn("test-session");
    when(session.getPrincipal())
        .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    when(identity.isCurrent(principal)).thenReturn(true);
    var transport = new InspectableTransport();
    guard.configureWebSocketTransport(transport);
    WebSocketHandler handler = transport.factories().getFirst().decorate(delegate);
    handler.afterConnectionEstablished(session);
    assertThat(guard.valid(session)).isTrue();
    when(identity.isCurrent(principal)).thenReturn(false);
    guard.closeRevokedSessions();
    verify(session).close(CloseStatus.POLICY_VIOLATION);
  }

  @Test
  void databaseFailureFailsClosedRatherThanDeliveringToUnverifiedSession() {
    var identity = mock(SessionIdentityService.class);
    var guard = new OrderSocketGuard(identity, mock(StoreAccessPolicy.class));
    var session = mock(WebSocketSession.class);
    var principal = new UtetraPrincipal(1L, 1L, "ROLE_USER", Instant.now().plusSeconds(300));
    when(session.getPrincipal())
        .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    when(identity.isCurrent(principal)).thenThrow(new IllegalStateException("DB unavailable"));
    assertThat(guard.valid(session)).isFalse();
  }

  private static class InspectableChannel
      extends org.springframework.messaging.simp.config.ChannelRegistration {
    java.util.List<org.springframework.messaging.support.ChannelInterceptor> filters() {
      return getInterceptors();
    }
  }

  @Test
  void removedStoreScopeClosesAnExistingSubscription() throws Exception {
    var identity = mock(SessionIdentityService.class);
    var stores = mock(StoreAccessPolicy.class);
    var guard = new OrderSocketGuard(identity, stores);
    var session = mock(WebSocketSession.class);
    var principal = new UtetraPrincipal(3L, 3L, "ROLE_MANAGER", Instant.now().plusSeconds(300));
    when(session.getId()).thenReturn("manager-session");
    when(session.getPrincipal())
        .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    when(identity.isCurrent(principal)).thenReturn(true);
    when(stores.canOperate(principal, 1L)).thenReturn(true);
    var transport = new InspectableTransport();
    guard.configureWebSocketTransport(transport);
    transport
        .factories()
        .getFirst()
        .decorate(mock(WebSocketHandler.class))
        .afterConnectionEstablished(session);
    var channel = new InspectableChannel();
    guard.configureClientInboundChannel(channel);
    var headers =
        org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(
            org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE);
    headers.setSessionId("manager-session");
    headers.setDestination("/topic/stores/1/orders");
    headers.setLeaveMutable(true);
    var message =
        org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], headers.getMessageHeaders());
    channel
        .filters()
        .getFirst()
        .preSend(message, mock(org.springframework.messaging.MessageChannel.class));
    assertThat(guard.valid(session)).isTrue();
    when(stores.canOperate(principal, 1L)).thenReturn(false);
    guard.closeRevokedSessions();
    verify(session).close(CloseStatus.POLICY_VIOLATION);
  }
}
