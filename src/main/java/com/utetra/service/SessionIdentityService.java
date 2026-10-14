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
        if (!principal.tokenExpiresAt().isAfter(clock.instant())) return false;
        return sessions.findByIdAndRevokedAtIsNullAndExpiresAtAfter(principal.sessionId(),clock.instant())
            .filter(session -> session.getUser().getId().equals(principal.userId()))
            .filter(session -> session.getUser().isActive() && session.getUser().getEmailVerifiedAt()!=null)
            .filter(session -> session.getUser().getRole().getName().equals(principal.role()))
            .isPresent();
    }
}
