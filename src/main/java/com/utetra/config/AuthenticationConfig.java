package com.utetra.config;
import com.utetra.repository.UserRepository;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationConfig {
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var user=users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
                .password(user.getPasswordHash()).authorities(user.getRole().getName())
                .disabled(!user.isActive() || user.getEmailVerifiedAt()==null).build();
        };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService details,PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
