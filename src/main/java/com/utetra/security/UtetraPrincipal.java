package com.utetra.security;
import java.security.Principal;
import java.time.Instant;

public record UtetraPrincipal(Long userId, Long sessionId, String role, Instant tokenExpiresAt) implements Principal {
    @Override public String getName() { return userId.toString(); }
}
