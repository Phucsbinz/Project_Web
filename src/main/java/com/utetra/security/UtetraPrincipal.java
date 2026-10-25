package com.utetra.security;
import java.security.Principal;
import java.time.Instant;

public record UtetraPrincipal(Long userId, Long sessionId, String role, Instant tokenExpiresAt, String tokenId) implements Principal {
    public UtetraPrincipal(Long userId,Long sessionId,String role,Instant tokenExpiresAt) {
        this(userId,sessionId,role,tokenExpiresAt,null);
    }
    @Override public String getName() { return userId.toString(); }
}
