package com.utetra.service;
import com.utetra.repository.AuthSessionRepository;
import com.utetra.security.UtetraPrincipal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionIdentityServiceTest {
    private static final Instant NOW=Instant.parse("2026-10-13T10:00:00Z");
    @Test void missingOrRevokedSessionIsRejected() {
        var repository=mock(AuthSessionRepository.class);
        when(repository.findByIdAndRevokedAtIsNullAndExpiresAtAfter(2L,NOW)).thenReturn(Optional.empty());
        var service=new SessionIdentityService(repository,Clock.fixed(NOW,ZoneOffset.UTC));
        assertThat(service.isCurrent(new UtetraPrincipal(1L,2L,"ROLE_USER",NOW.plusSeconds(60)))).isFalse();
    }
    @Test void expiredTokenDoesNotReachRepository() {
        var repository=mock(AuthSessionRepository.class);
        var service=new SessionIdentityService(repository,Clock.fixed(NOW,ZoneOffset.UTC));
        assertThat(service.isCurrent(new UtetraPrincipal(1L,2L,"ROLE_USER",NOW.minusSeconds(1)))).isFalse();
        verifyNoInteractions(repository);
    }
}
