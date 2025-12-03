package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Feign client for User Service
 * Automatically propagates JWT tokens for authenticated requests
 */
@FeignClient(
    name = "user-service", 
    url = "${feign.services.user-service.url}",
    configuration = com.tala.core.feign.FeignJwtConfig.class
)
public interface UserServiceClient {
    
    @GetMapping("/api/v1/profiles/{id}")
    ProfileResponse getProfile(@PathVariable Long id);
    
    @GetMapping("/api/v1/users/interest/scores")
    InterestScoresResponse getInterestScores(
        @RequestParam Long userId,
        @RequestParam Long profileId
    );
    
    class ProfileResponse {
        public Long id;
        public Long userId;
        public String childName;
        public LocalDate birthDate;
        public String gender;
        public Integer ageMonths;
    }
    
    class InterestScoresResponse {
        public Long profileId;
        public Map<String, Double> interestVector;
        public List<String> explicitTopics;
        public List<String> recentTopics;
    }
}
