package com.utetra.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
/** Resolve deferred CSRF before JSP/SiteMesh can commit the response headers. */
public class CsrfTokenMaterializationFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(req.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) token.getToken();
        chain.doFilter(req,res);
    }
}
