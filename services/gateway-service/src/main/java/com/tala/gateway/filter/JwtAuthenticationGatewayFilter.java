package com.tala.gateway.filter;

import com.tala.core.security.JwtConstants;
import com.tala.core.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Gateway Filter
 * Validates JWT tokens and adds user context headers to downstream services
 */
@Component
@Slf4j
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {
    
    @Value("${jwt.secret:dev-secret-key-change-in-production-minimum-64-characters-long}")
    private String jwtSecret;
    
    // Public paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/actuator/health",
        "/actuator/info"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // Allow CORS preflight requests without authentication
        if (HttpMethod.OPTIONS.equals(method)) {
            log.debug("CORS preflight request for path: {}", path);
            return chain.filter(exchange);
        }
        
        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Public path accessed: {}", path);
            return chain.filter(exchange);
        }
        
        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(JwtConstants.AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }
        
        // Extract and validate token
        String token = JwtUtils.extractTokenFromHeader(authHeader);
        
        if (token == null || !JwtUtils.validateToken(token, jwtSecret)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
        
        // Verify it's an access token (not refresh token)
        if (!JwtUtils.isAccessToken(token, jwtSecret)) {
            log.warn("Token is not an access token for path: {}", path);
            return onError(exchange, "Invalid token type", HttpStatus.UNAUTHORIZED);
        }
        
        // Extract user information from token
        Long userId = JwtUtils.getUserIdFromToken(token, jwtSecret);
        String email = JwtUtils.getEmailFromToken(token, jwtSecret);
        
        if (userId == null || email == null) {
            log.warn("Failed to extract user info from token for path: {}", path);
            return onError(exchange, "Invalid token claims", HttpStatus.UNAUTHORIZED);
        }
        
        // Add user context headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(JwtConstants.USER_ID_HEADER, String.valueOf(userId))
                .header(JwtConstants.USER_EMAIL_HEADER, email)
                .build();
        
        log.debug("Authenticated request for user: {} (ID: {}) to path: {}", email, userId, path);
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", 
                status.getReasonPhrase(), message);
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes())));
    }
}
