package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth; private final OtpService otp; private final AuthCookies cookies; private final IdentityRateLimiter limits;
    public AuthController(AuthService auth,OtpService otp,AuthCookies cookies,IdentityRateLimiter limits) { this.auth=auth; this.otp=otp; this.cookies=cookies; this.limits=limits; }
    @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody IdentityForms.Register form,HttpServletRequest req) {
        limits.check("otp:"+req.getRemoteAddr(),30); auth.register(form); return Map.of("message","Đã gửi OTP kích hoạt qua email.");
    }
    @PostMapping("/otp/send") public Map<String,Object> resend(@Valid @RequestBody IdentityForms.SendOtp form,HttpServletRequest req) {
        limits.check("otp:"+req.getRemoteAddr(),30); otp.request(form.email(),form.purpose()); return generic();
    }
    @PostMapping("/forgot") public Map<String,Object> forgot(@Valid @RequestBody IdentityForms.EmailRequest form,HttpServletRequest req) {
        limits.check("otp:"+req.getRemoteAddr(),30); otp.request(form.email(),"PASSWORD_RESET"); return generic();
    }
    @PostMapping("/otp/verify") public Map<String,Object> verify(@Valid @RequestBody IdentityForms.Verify form,HttpServletRequest req,HttpServletResponse res) {
        limits.check("verify:"+req.getRemoteAddr(),60); String grant=otp.verify(form.email(),form.purpose(),form.otp());
        res.setHeader("Cache-Control","no-store");
        return Map.of("message",grant.isEmpty()?"Đã kích hoạt tài khoản. Bạn có thể đăng nhập.":"OTP hợp lệ. Vui lòng đặt mật khẩu mới.","grant",grant);
    }
    @PostMapping("/reset") public Map<String,Object> reset(@Valid @RequestBody IdentityForms.Reset form,HttpServletRequest req,HttpServletResponse res) {
        limits.check("reset:"+req.getRemoteAddr(),30); auth.reset(form.grant(),form.password()); cookies.clear(req,res); return Map.of("message","Đã đổi mật khẩu và thu hồi tất cả phiên.");
    }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody IdentityForms.Login form,HttpServletRequest req,HttpServletResponse res) {
        limits.check("login:"+req.getRemoteAddr(),60); var result=auth.login(form.email(),form.password()); cookies.write(req,res,result); cookies.rotateCsrf(req,res); return Map.of("message","Đăng nhập thành công.","redirect",req.getContextPath()+LoginDestination.resolve(result.role(),req.getParameter("next")));
    }
    @PostMapping("/refresh") public Map<String,Object> refresh(@CookieValue(name="UTETRA_REFRESH",required=false) String token,HttpServletRequest req,HttpServletResponse res) {
        limits.check("refresh:"+req.getRemoteAddr(),120); cookies.write(req,res,auth.refresh(token)); return Map.of("message","Đã làm mới phiên.");
    }
    @PostMapping("/logout") public Map<String,Object> logout(@AuthenticationPrincipal UtetraPrincipal p,@CookieValue(name="UTETRA_REFRESH",required=false) String token,HttpServletRequest req,HttpServletResponse res) {
        auth.logout(p,token); cookies.clear(req,res); return Map.of("message","Đã đăng xuất.","redirect",req.getContextPath()+"/auth/login");
    }
    private Map<String,Object> generic() { return Map.of("message","Nếu tài khoản đủ điều kiện, email OTP sẽ được gửi. Vui lòng kiểm tra hộp thư."); }
}
