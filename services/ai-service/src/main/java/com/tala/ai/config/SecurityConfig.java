package com.tala.ai.config;

import com.tala.core.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for AI Service
 * 
 * Validates JWT tokens for all incoming requests.
 * Supports both gateway-forwarded requests (with X-User-Id header)
 * and direct requests (with Authorization header).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()  // Allow error page
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Disable exception handling for async requests (SSE)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    if (!response.isCommitted()) {
                        response.sendError(401, "Unauthorized");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (!response.isCommitted()) {
                        response.sendError(403, "Forbidden");
                    }
                })
            );
        
        return http.build();
    }
}
