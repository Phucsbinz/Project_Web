package com.utetra.config;

import com.utetra.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter jwt) throws Exception {
        // JWT is authenticated on every request. Do not treat each request (including
        // assets) as a new login and rotate the CSRF cookie behind the rendered page.
        // Authentication is reconstructed from JWT, never restored from an HTTP session.
        // The identity login/logout service must rotate CSRF at those actual boundaries.
        http.sessionManagement(session -> session.disable())
            .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
            .addFilterBefore(new DisableEncodeUrlFilter(),SecurityContextHolderFilter.class)
            .requestCache(cache -> cache.disable())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD,DispatcherType.ERROR).permitAll()
                .requestMatchers("/","/menu","/products/**","/stores/**","/assets/**","/auth/**","/error","/api/system/health").permitAll()
                .requestMatchers("/admin/**","/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**","/api/manager/**").hasAnyRole("MANAGER","ADMIN")
                .requestMatchers("/vendor/**","/api/vendor/**").hasRole("VENDOR")
                .requestMatchers("/shipper/**","/api/shipper/**").hasRole("SHIPPER")
                .requestMatchers("/account/profile","/account/password").authenticated()
                .requestMatchers("/cart/**").permitAll()
                .requestMatchers("/checkout/**","/orders/**","/account/**","/favorites/**","/viewed/**","/reviews/**","/comments/**","/api/buyer/**").hasAnyRole("USER","VENDOR")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.authenticationEntryPoint((req,res,ex) -> {
                if (req.getRequestURI().startsWith(req.getContextPath()+"/api/") || req.getRequestURI().equals(req.getContextPath()+"/ws")) {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                } else res.sendRedirect(req.getContextPath()+"/auth/login");
            }))
            .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        var bean=new FilterRegistrationBean<>(filter); bean.setEnabled(false); return bean;
    }
}
