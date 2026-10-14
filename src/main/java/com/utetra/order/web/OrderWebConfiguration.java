package com.utetra.order.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class OrderWebConfiguration {
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> orderCsrfCookieMaterializer() {
    var filter =
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            // Security installs a deferred token. SiteMesh decorators can render after
            // headers are committed, so materialize its cookie before SiteMesh (order 1).
            Object value = request.getAttribute(CsrfToken.class.getName());
            if (value instanceof CsrfToken token) token.getToken();
            chain.doFilter(request, response);
          }
        };
    var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
    registration.setName("orderCsrfCookieMaterializer");
    registration.setOrder(0);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
