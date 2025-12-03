package com.tala.core.feign;

import com.tala.core.security.JwtConstants;
import com.tala.core.security.JwtContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Request Interceptor for JWT Token Propagation
 * 
 * Automatically propagates the JWT token from the incoming request
 * to downstream microservice calls.
 * 
 * Industry Best Practice: 
 * - Extract JWT from current request context or thread-local context
 * - Pass it along in Authorization header to downstream services
 * - Maintains authentication chain across service boundaries
 */
@Slf4j
public class JwtFeignRequestInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        String targetService = template.feignTarget() != null ? template.feignTarget().name() : "unknown";
        String targetUrl = template.url();
        
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Propagate Authorization header (JWT token)
                String authHeader = request.getHeader(JwtConstants.AUTHORIZATION_HEADER);
                if (authHeader != null && !authHeader.isEmpty()) {
                    template.header(JwtConstants.AUTHORIZATION_HEADER, authHeader);
                    log.debug("JWT token propagated to service: {} at {}", targetService, targetUrl);
                } else {
                    log.warn("No JWT token found in request context for service call to: {} at {}", 
                            targetService, targetUrl);
                }
                
                // Optionally propagate user headers if they exist (from gateway)
                String userId = request.getHeader(JwtConstants.USER_ID_HEADER);
                if (userId != null && !userId.isEmpty()) {
                    template.header(JwtConstants.USER_ID_HEADER, userId);
                    log.debug("User ID header propagated: {}", userId);
                }
                
                String userEmail = request.getHeader(JwtConstants.USER_EMAIL_HEADER);
                if (userEmail != null && !userEmail.isEmpty()) {
                    template.header(JwtConstants.USER_EMAIL_HEADER, userEmail);
                }
            } else {
                // Fallback to thread-local JWT context (for async threads)
                String jwtToken = JwtContextHolder.getToken();
                if (jwtToken != null && !jwtToken.isEmpty()) {
                    template.header(JwtConstants.AUTHORIZATION_HEADER, jwtToken);
                    log.debug("JWT token propagated from thread-local context to service: {} at {}", 
                            targetService, targetUrl);
                } else {
                    log.warn("No request context and no thread-local JWT token available for service call to: {} at {}", 
                            targetService, targetUrl);
                }
            }
        } catch (Exception e) {
            log.error("Failed to propagate JWT token to service: {} at {}, error: {}", 
                    targetService, targetUrl, e.getMessage(), e);
            // Continue without JWT - let the downstream service handle authentication failure
        }
    }
}
