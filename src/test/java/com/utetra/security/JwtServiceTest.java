package com.utetra.security;
import java.time.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final String KEY=Base64.getEncoder().encodeToString("test-only-key-012345678901234567890123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final Instant NOW=Instant.parse("2026-10-13T10:00:00Z");
    private JwtService service(Instant instant,String issuer,String audience) {
        return new JwtService(KEY,issuer,audience,Duration.ofMinutes(15),Clock.fixed(instant,ZoneOffset.UTC));
    }
    @Test void verifiesSignedIdentity() {
        var jwt=service(NOW,"utetra","utetra-web");
        var principal=jwt.verify(jwt.issue(1L,2L,"ROLE_VENDOR"));
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.sessionId()).isEqualTo(2L);
        assertThat(principal.role()).isEqualTo("ROLE_VENDOR");
    }
    @Test void rejectsExpiredToken() {
        String token=service(NOW,"utetra","utetra-web").issue(1L,2L,"ROLE_USER");
        assertThatThrownBy(() -> service(NOW.plusSeconds(901),"utetra","utetra-web").verify(token)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsTamperedPayload() {
        var jwt=service(NOW,"utetra","utetra-web");
        String token=jwt.issue(1L,2L,"ROLE_USER");
        String[] segments=token.split("\\.");
        String payload=new String(Base64.getUrlDecoder().decode(segments[1]),java.nio.charset.StandardCharsets.UTF_8).replace("ROLE_USER","ROLE_ADMIN");
        String tampered=segments[0]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+segments[2];
        assertThatThrownBy(() -> jwt.verify(tampered)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsWrongIssuerAndAudience() {
        String token=service(NOW,"utetra","utetra-web").issue(1L,2L,"ROLE_USER");
        assertThatThrownBy(() -> service(NOW,"other","utetra-web").verify(token)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(NOW,"utetra","other").verify(token)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsWeakKey() {
        assertThatThrownBy(() -> new JwtService(Base64.getEncoder().encodeToString(new byte[8]),"utetra","utetra-web",Duration.ofMinutes(15),Clock.systemUTC()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
