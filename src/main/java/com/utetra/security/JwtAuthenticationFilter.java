package com.utetra.security;

import com.utetra.service.SessionIdentityService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final SessionIdentityService identity;
    public JwtAuthenticationFilter(JwtService jwt,SessionIdentityService identity) { this.jwt=jwt; this.identity=identity; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String token=null;
        String header=request.getHeader("Authorization");
        if (header!=null && header.regionMatches(true,0,"Bearer ",0,7)) token=header.substring(7).trim();
        else if (request.getCookies()!=null) {
            for (Cookie cookie:request.getCookies()) if ("UTETRA_JWT".equals(cookie.getName())) { token=cookie.getValue(); break; }
        }
        if (token!=null) {
            try {
                var principal=jwt.verify(token);
                if (!identity.isCurrent(principal)) throw new IllegalArgumentException("Revoked session");
                var authentication=new UsernamePasswordAuthenticationToken(principal,null,
                    List.of(new SimpleGrantedAuthority(principal.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                if (request.getRequestURI().startsWith(request.getContextPath()+"/api/")) {
                    response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                    return;
                }
                response.addHeader("Set-Cookie","UTETRA_JWT=; Path="+(request.getContextPath().isEmpty()?"/":request.getContextPath())+"; Max-Age=0; HttpOnly; SameSite=Lax");
            }
        }
        chain.doFilter(request,response);
    }
}
