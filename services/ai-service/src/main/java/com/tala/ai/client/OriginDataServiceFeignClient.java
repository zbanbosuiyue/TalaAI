package com.tala.ai.client;

import com.tala.core.feign.FeignJwtConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client for Origin Data Service
 * 
 * Automatically propagates JWT tokens from incoming requests to origin-data-service.
 * Uses FeignJwtConfig for JWT token propagation.
 */
@FeignClient(
    name = "origin-data-service",
    url = "${services.origin-data-service.url}",
    configuration = FeignJwtConfig.class
)
public interface OriginDataServiceFeignClient {
    
    /**
     * Send chat event to origin-data-service
     */
    @PostMapping("/api/v1/chat-events")
    ChatEventResponse sendChatEvent(@RequestBody Object chatEventRequest);
    
    /**
     * Health check for origin-data-service
     */
    @GetMapping("/api/v1/chat-events/health")
    HealthResponse health();
    
    /**
     * Chat Event Response DTO
     */
    class ChatEventResponse {
        public Long id;
        public String status;
        public String message;
    }
    
    /**
     * Health Response DTO
     */
    class HealthResponse {
        public String status;
    }
}
