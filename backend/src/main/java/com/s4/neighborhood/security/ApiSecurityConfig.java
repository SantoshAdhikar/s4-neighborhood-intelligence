
package com.s4.neighborhood.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(request -> {
                    String path = request.getRequestURI();

                    return path.equals("/api/businesses")
                            || path.startsWith("/api/businesses/");
                }))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health")
                        .permitAll()
                        .requestMatchers(
                                "/api/businesses",
                                "/api/businesses/**"
                        )
                        .permitAll()
                        .anyRequest()
                        .denyAll()
                )
                .build();
    }
}
