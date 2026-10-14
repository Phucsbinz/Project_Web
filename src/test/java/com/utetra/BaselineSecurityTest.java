package com.utetra;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BaselineSecurityTest {
    @Autowired MockMvc mvc;
    @Test void guestHealthIsJsonAndNotDecorated() throws Exception {
        mvc.perform(get("/api/system/health")).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.application").value("UTETra"));
    }
    @Test void guestCannotReadAdmin() throws Exception { mvc.perform(get("/api/admin/system")).andExpect(status().isUnauthorized()); }
    @Test @WithMockUser(roles="USER") void buyerCannotReadAdmin() throws Exception { mvc.perform(get("/api/admin/system")).andExpect(status().isForbidden()); }
    @Test @WithMockUser(roles="ADMIN") void adminCanReadAdmin() throws Exception { mvc.perform(get("/api/admin/system")).andExpect(status().isOk()); }
    @Test void forgedBearerIsRejected() throws Exception { mvc.perform(get("/api/admin/system").header("Authorization","Bearer forged")).andExpect(status().isUnauthorized()); }
    @Test void missingCsrfDoesNotChangeData() throws Exception { mvc.perform(post("/auth/unimplemented")).andExpect(status().isForbidden()); }
}
