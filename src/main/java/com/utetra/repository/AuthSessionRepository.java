package com.utetra.repository;
import com.utetra.entity.AuthSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface AuthSessionRepository extends JpaRepository<AuthSession,Long> {
    @EntityGraph(attributePaths={"user","user.role"})
    Optional<AuthSession> findByIdAndRevokedAtIsNullAndExpiresAtAfter(Long id, Instant now);
}
