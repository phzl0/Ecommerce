package com.paulo.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditoriaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || auth.getName().equals("anonymousUser")) {
                return Optional.of("sistema");
            }
            return Optional.of(auth.getName());
        };
    }
}