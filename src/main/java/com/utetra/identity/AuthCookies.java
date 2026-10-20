package com.utetra.identity;
import java.time.Duration;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
@Component
public class AuthCookies {
    private final boolean secure; private final Duration access,refresh;
    public AuthCookies(@Value("${app.cookie.secure}") boolean secure,@Value("${security.jwt.access-duration}") Duration access,@Value("${security.jwt.refresh-duration}") Duration refresh) { this.secure=secure; this.access=access; this.refresh=refresh; }
    public void write(HttpServletRequest req,HttpServletResponse res,AuthService.Tokens tokens) {
        set(req,res,"UTETRA_JWT",tokens.access(),access); set(req,res,"UTETRA_REFRESH",tokens.refresh(),refresh);
    }
    public void rotateCsrf(HttpServletRequest req,HttpServletResponse res) {
        org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null,req,res);
    }
    public void clear(HttpServletRequest req,HttpServletResponse res) { set(req,res,"UTETRA_JWT","",Duration.ZERO); set(req,res,"UTETRA_REFRESH","",Duration.ZERO); rotateCsrf(req,res); }
    private void set(HttpServletRequest req,HttpServletResponse res,String name,String value,Duration age) {
        String path=req.getContextPath().isEmpty()?"/":req.getContextPath();
        res.addHeader("Set-Cookie",ResponseCookie.from(name,value).httpOnly(true).secure(secure).sameSite("Lax").path(path).maxAge(age).build().toString());
        res.setHeader("Cache-Control","no-store");
    }
}
