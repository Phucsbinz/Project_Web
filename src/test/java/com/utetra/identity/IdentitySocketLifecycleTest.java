package com.utetra.identity;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.utetra.service.SessionIdentityService;
import com.utetra.security.UtetraPrincipal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.*;
class IdentitySocketLifecycleTest {
    @Test @SuppressWarnings("unchecked") void closesIdleRevokedSocket() throws Exception {
        var identity=mock(SessionIdentityService.class); var lifecycle=new IdentitySocketLifecycle(identity);
        var socket=mock(WebSocketSession.class); var p=new UtetraPrincipal(1L,2L,"ROLE_USER",Instant.now().plusSeconds(60));
        when(socket.getId()).thenReturn("socket"); when(socket.getPrincipal()).thenReturn(new UsernamePasswordAuthenticationToken(p,null));
        var sessions=(Map<String,WebSocketSession>)ReflectionTestUtils.getField(lifecycle,"sessions"); sessions.put("socket",socket);
        lifecycle.closeRevoked(); verify(socket).close(CloseStatus.POLICY_VIOLATION); assertThat(sessions).isEmpty();
    }
}
