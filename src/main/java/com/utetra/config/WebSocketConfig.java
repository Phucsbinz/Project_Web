package com.utetra.config;

import com.utetra.security.UtetraPrincipal;
import com.utetra.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final SessionIdentityService identity;
    private final StoreAccessPolicy stores;
    public WebSocketConfig(SessionIdentityService identity,StoreAccessPolicy stores) { this.identity=identity; this.stores=stores; }
    @Bean @ConditionalOnMissingBean(StoreAccessPolicy.class)
    static StoreAccessPolicy defaultStoreAccessPolicy() { return (principal,storeId) -> false; }
    @Override public void registerStompEndpoints(StompEndpointRegistry endpoints) { endpoints.addEndpoint("/ws"); }
    @Override public void configureMessageBroker(MessageBrokerRegistry broker) {
        broker.enableSimpleBroker("/topic","/queue");
        broker.setApplicationDestinationPrefixes("/app"); broker.setUserDestinationPrefix("/user");
    }
    @Override public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message,MessageChannel channel) {
                var accessor=MessageHeaderAccessor.getAccessor(message,SimpMessageHeaderAccessor.class);
                if (accessor==null || accessor.getMessageType()==SimpMessageType.DISCONNECT) return message;
                if (!(accessor.getUser() instanceof Authentication auth)
                    || !(auth.getPrincipal() instanceof UtetraPrincipal principal) || !identity.isCurrent(principal))
                    throw new org.springframework.security.access.AccessDeniedException("Session expired or revoked");
                if (accessor.getMessageType()==SimpMessageType.MESSAGE)
                    throw new org.springframework.security.access.AccessDeniedException("Order mutations must use HTTP services");
                if (accessor.getMessageType()==SimpMessageType.SUBSCRIBE && !allowed(principal,accessor.getDestination()))
                    throw new org.springframework.security.access.AccessDeniedException("Destination is not authorized");
                return message;
            }
        });
    }
    private boolean allowed(UtetraPrincipal principal,String destination) {
        if("/user/queue/support".equals(destination)) return java.util.Set.of("ROLE_USER","ROLE_MANAGER","ROLE_ADMIN").contains(principal.role());
        // These topics contain public availability IDs only, never order/identity data.
        if("/topic/checkout/promotions".equals(destination)
            || destination!=null && destination.matches("/topic/stores/[0-9]+/checkout")) return true;
        if ("/user/queue/orders".equals(destination) || "/user/queue/notifications".equals(destination)) return true;
        if ("/topic/admin/orders".equals(destination)) return "ROLE_ADMIN".equals(principal.role());
        if (destination!=null && destination.matches("/topic/stores/[0-9]+/orders")) {
            if ("ROLE_ADMIN".equals(principal.role())) return true;
            return stores.canOperate(principal,Long.parseLong(destination.split("/")[3]));
        }
        return false;
    }
}
