package com.utetra.service;

import com.utetra.repository.AuthSessionRepository;
import com.utetra.security.UtetraPrincipal;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionIdentityService {
    private final AuthSessionRepository sessions;
    private final Clock clock;
    public SessionIdentityService(AuthSessionRepository sessions, Clock clock) { this.sessions=sessions; this.clock=clock; }
    @Transactional(readOnly=true)
    public boolean isCurrent(UtetraPrincipal principal) {
        return current(principal,null);
    }
    @Transactional(readOnly=true)
    public boolean isCurrent(UtetraPrincipal principal,String token) {
        return current(principal,token);
    }
    private boolean current(UtetraPrincipal principal,String token) {
        if (principal.tokenId()==null || !principal.tokenExpiresAt().isAfter(clock.instant())) return false;
        return sessions.findByIdAndRevokedAtIsNullAndExpiresAtAfter(principal.sessionId(),clock.instant())
            .filter(session -> session.getUser().getId().equals(principal.userId()))
            .filter(session -> session.getUser().isActive() && session.getUser().getEmailVerifiedAt()!=null)
            .filter(session -> session.getUser().getRole().getName().equals(principal.role()))
            .filter(session -> principal.tokenId().equals(session.getAccessTokenId()))
            .filter(session -> session.getAccessExpiresAt()!=null && session.getAccessExpiresAt().isAfter(clock.instant()))
            .filter(session -> token==null || java.security.MessageDigest.isEqual(
                com.utetra.repository.AccessTokenRegistry.fingerprint(token).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                java.util.Objects.toString(session.getAccessTokenHash(),"").getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            .isPresent();
    }
}
