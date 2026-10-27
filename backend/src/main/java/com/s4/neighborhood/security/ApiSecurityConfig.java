package com.s4.neighborhood.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Hackathon demo only: onboarding is unauthenticated. Add owner authentication before deployment.
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/businesses", "/api/businesses/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/businesses", "/api/businesses/**").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
