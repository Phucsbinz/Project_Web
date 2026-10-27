package com.utetra.config;
import org.springframework.context.annotation.*;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration @EnableWebSocketSecurity
public class WebSocketSecurityConfig {
    @Bean AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages.nullDestMatcher().authenticated()
            .simpSubscribeDestMatchers("/user/queue/orders","/user/queue/notifications","/user/queue/support","/topic/stores/*/orders","/topic/admin/orders","/topic/stores/*/checkout","/topic/checkout/promotions").authenticated()
            .anyMessage().denyAll();
        return messages.build();
    }
}
