package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.SessionIdentityService;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
/** Also closes idle sockets on revocation/expiry; inbound checks alone do not stop subscriptions. */
@Configuration @EnableScheduling
public class IdentitySocketLifecycle implements WebSocketMessageBrokerConfigurer {
    private final ConcurrentHashMap<String,WebSocketSession> sessions=new ConcurrentHashMap<>();
    private final SessionIdentityService identity;
    public IdentitySocketLifecycle(SessionIdentityService identity) { this.identity=identity; }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(),session); super.afterConnectionEstablished(session);
            }
            @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) throws Exception {
                sessions.remove(session.getId()); super.afterConnectionClosed(session,status);
            }
        });
    }
    @Scheduled(fixedDelay=1000)
    public void closeRevoked() {
        sessions.values().forEach(session -> {
            boolean valid=false;
            try { valid=session.getPrincipal() instanceof Authentication a && a.getPrincipal() instanceof UtetraPrincipal p && identity.isCurrent(p); }
            catch(RuntimeException ignored) { /* fail closed if database cannot verify a session */ }
            if(!valid) { try { session.close(CloseStatus.POLICY_VIOLATION); } catch(Exception ignored) {} sessions.remove(session.getId()); }
        });
    }
}
