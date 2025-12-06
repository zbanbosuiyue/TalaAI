package com.tala.core.config;

import com.tala.core.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Base Security Configuration for all Tala microservices
 * 
 * Provides common security setup:
 * - JWT authentication filter
 * - CSRF disabled (stateless API)
 * - Stateless session management
 * - Actuator endpoints public
 * - Error endpoint public
 * 
 * Usage:
 * 1. Extend this class in your service's SecurityConfig
 * 2. Override configureServiceSpecificSecurity() to add service-specific rules
 * 3. Call super.configureCommonSecurity(http) first
 * 
 * Example:
 * <pre>
 * @Configuration
 * @EnableWebSecurity
 * public class MyServiceSecurityConfig extends BaseSecurityConfig {
 *     
 *     @Bean
 *     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         configureCommonSecurity(http);
 *         
 *         http.authorizeHttpRequests(auth -> auth
 *             .requestMatchers("/api/v1/public/**").permitAll()
 *             .anyRequest().authenticated()
 *         );
 *         
 *         return http.build();
 *     }
 * }
 * </pre>
 */
@RequiredArgsConstructor
public abstract class BaseSecurityConfig {
    
    protected final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * Configure common security settings shared across all services
     */
    protected void configureCommonSecurity(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    /**
     * Configure exception handling for async/SSE endpoints
     * Call this if your service has SSE or async endpoints
     */
    protected void configureAsyncExceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling(exception -> exception
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
    }
}
