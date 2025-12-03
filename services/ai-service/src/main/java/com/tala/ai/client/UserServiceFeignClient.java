package com.tala.ai.client;

import com.tala.core.feign.FeignJwtConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;

/**
 * Feign Client for User Service
 * 
 * Automatically propagates JWT tokens from incoming requests to user-service.
 * Uses FeignJwtConfig for JWT token propagation.
 */
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignJwtConfig.class
)
public interface UserServiceFeignClient {
    
    /**
     * Get baby profile by ID
     */
    @GetMapping("/api/v1/profiles/{id}")
    ProfileResponse getProfile(@PathVariable("id") Long id);
    
    /**
     * Health check for user-service
     */
    @GetMapping("/actuator/health")
    HealthResponse health();
    
    /**
     * Profile Response DTO
     */
    class ProfileResponse {
        public Long id;
        public Long userId;
        public String babyName;
        public LocalDate birthDate;
        public String timezone;
        public String gender;
        public String photoUrl;
        public String parentName;
        public String parentRole;
        public String zipcode;
        public String concerns;
        public Boolean hasDaycare;
        public String daycareName;
        public String updateMethod;
        public Integer ageInDays;
        public Boolean isDefault;
    }
    
    /**
     * Health Response DTO
     */
    class HealthResponse {
        public String status;
    }
}
