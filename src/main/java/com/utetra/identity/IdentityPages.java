package com.utetra.identity;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class IdentityPages {
    @GetMapping("/admin/moderation")
    public String moderation(HttpServletResponse response) { response.setHeader("Cache-Control","no-store"); return "identity/moderation"; }
    @GetMapping({"/auth/register","/auth/otp","/auth/forgot","/auth/reset","/account/profile","/account/security","/account/addresses","/account/loyalty","/favorites","/viewed","/reviews","/comments","/manager/users","/admin/users"})
    public String page(HttpServletRequest req,HttpServletResponse res,Model model) {
        if(req.getUserPrincipal() instanceof org.springframework.security.core.Authentication a && a.getPrincipal() instanceof com.utetra.security.UtetraPrincipal p) {
            model.addAttribute("identityRole",p.role());
            model.addAttribute("buyer",java.util.Set.of("ROLE_USER","ROLE_VENDOR").contains(p.role()));
        }
        res.setHeader("Cache-Control","no-store"); model.addAttribute("identityPage",req.getServletPath()); return "identity/page";
    }
}
