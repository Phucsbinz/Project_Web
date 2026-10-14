package com.utetra.config;
import com.cloudinary.Cloudinary;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class CloudinaryConfig {
    @Bean @ConditionalOnProperty(name="cloudinary.enabled",havingValue="true")
    Cloudinary cloudinary(@Value("${cloudinary.cloud-name}") String name,
            @Value("${cloudinary.api-key}") String key,@Value("${cloudinary.api-secret}") String secret) {
        if (name.isBlank() || key.isBlank() || secret.isBlank()) throw new IllegalArgumentException("Cloudinary credentials are required when enabled");
        return new Cloudinary(Map.of("cloud_name",name,"api_key",key,"api_secret",secret,"secure",true));
    }
}
