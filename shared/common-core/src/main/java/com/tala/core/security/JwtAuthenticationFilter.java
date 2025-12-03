package com.tala.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter for servlet-based applications
 * Validates JWT tokens and sets Spring Security context
 * 
 * NOTE: Only loaded in SERVLET applications, NOT in reactive (WebFlux) applications like gateway-service
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Value("${jwt.secret:dev-secret-key-change-in-production-minimum-64-characters-long}")
    private String jwtSecret;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            String authHeader = request.getHeader(JwtConstants.AUTHORIZATION_HEADER);
            
            // Store JWT token in thread-local context for downstream service calls
            if (authHeader != null && authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
                JwtContextHolder.setToken(authHeader);
            }
            
            // Check for user ID header from gateway (already validated)
            String userIdHeader = request.getHeader(JwtConstants.USER_ID_HEADER);
            if (userIdHeader != null) {
                // Request came through gateway and was already validated
                Long userId = Long.parseLong(userIdHeader);
                setAuthentication(request, userId);
                filterChain.doFilter(request, response);
                return;
            }
            
            // Direct request - validate JWT
            if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String token = JwtUtils.extractTokenFromHeader(authHeader);
            
            if (token != null && JwtUtils.validateToken(token, jwtSecret)) {
                // Verify it's an access token
                if (!JwtUtils.isAccessToken(token, jwtSecret)) {
                    log.warn("Token is not an access token");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                Long userId = JwtUtils.getUserIdFromToken(token, jwtSecret);
                String email = JwtUtils.getEmailFromToken(token, jwtSecret);
                
                if (userId != null && email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    setAuthentication(request, userId);
                    log.debug("Set authentication for user: {} (ID: {})", email, userId);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clean up JWT context after request completes
                JwtContextHolder.clear();
            }
        }
    }
    
    private void setAuthentication(HttpServletRequest request, Long userId) {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.emptyList()
            );
        
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for public endpoints
        return path.startsWith("/api/v1/auth/") || 
               path.startsWith("/actuator/") ||
               path.equals("/error");  // Skip error page
    }
}
