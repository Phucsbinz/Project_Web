package com.utetra.order.service;

import com.utetra.security.UtetraPrincipal;
import com.utetra.service.SessionIdentityService;
import com.utetra.service.StoreAccessPolicy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/** Additive transport guard: revoked sessions/scopes cannot keep receiving broker messages. */
@Configuration
@EnableScheduling
public class OrderSocketGuard implements WebSocketMessageBrokerConfigurer {
  private final SessionIdentityService identity;
  private final StoreAccessPolicy stores;
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final Map<String, Set<Long>> subscribedStores = new ConcurrentHashMap<>();

  public OrderSocketGuard(SessionIdentityService identity, StoreAccessPolicy stores) {
    this.identity = identity;
    this.stores = stores;
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration transport) {
    transport.addDecoratorFactory(
        handler ->
            new WebSocketHandlerDecorator(handler) {
              @Override
              public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                super.afterConnectionEstablished(session);
              }

              @Override
              public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
                  throws Exception {
                sessions.remove(session.getId());
                subscribedStores.remove(session.getId());
                super.afterConnectionClosed(session, status);
              }
            });
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration channel) {
    channel.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel target) {
            var h = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
            if (h != null
                && h.getMessageType() == SimpMessageType.SUBSCRIBE
                && h.getSessionId() != null
                && h.getDestination() != null
                && h.getDestination().matches("/topic/stores/[0-9]+/orders")) {
              try {
                long store = Long.parseLong(h.getDestination().split("/")[3]);
                subscribedStores
                    .computeIfAbsent(h.getSessionId(), id -> ConcurrentHashMap.newKeySet())
                    .add(store);
              } catch (NumberFormatException e) {
                throw OrderProblem.forbidden();
              }
            }
            return message;
          }
        });
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration channel) {
    channel.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel target) {
            var h = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
            if (h == null || h.getSessionId() == null) return message;
            WebSocketSession session = sessions.get(h.getSessionId());
            if (session != null && !valid(session)) {
              close(session);
              return null;
            }
            return message;
          }
        });
  }

  public boolean valid(WebSocketSession session) {
    try {
      if (!(session.getPrincipal() instanceof Authentication auth)
          || !(auth.getPrincipal() instanceof UtetraPrincipal p)
          || !identity.isCurrent(p)) return false;
      for (long store : subscribedStores.getOrDefault(session.getId(), Set.of()))
        if (!"ROLE_ADMIN".equals(p.role()) && !stores.canOperate(p, store)) return false;
      return true;
    } catch (RuntimeException failure) {
      return false;
    }
  }

  private void close(WebSocketSession session) {
    try {
      session.close(CloseStatus.POLICY_VIOLATION);
    } catch (java.io.IOException ignored) {
    } finally {
      sessions.remove(session.getId());
      subscribedStores.remove(session.getId());
    }
  }

  @Scheduled(fixedDelay = 5000)
  public void closeRevokedSessions() {
    for (var session : sessions.values()) if (!valid(session)) close(session);
  }
}
