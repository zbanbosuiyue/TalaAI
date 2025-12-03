package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for AI Service
 * Automatically propagates JWT tokens for authenticated requests
 */
@FeignClient(
    name = "ai-service", 
    url = "${feign.services.ai-service.url}",
    configuration = com.tala.core.feign.FeignJwtConfig.class
)
public interface AIServiceClient {
    
    @GetMapping("/api/v1/ai/today-overview")
    TodayOverviewResponse getTodayOverview(
        @RequestParam Long profileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
    
    @GetMapping("/api/v1/ai/ask-baby-suggestions")
    List<String> getAskBabySuggestions(
        @RequestParam Long profileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
    
    class TodayOverviewResponse {
        public Long profileId;
        public LocalDate date;
        public String summarySentence;
        public String actionSuggestion;
        public List<PillTopic> pillTopics;
    }
    
    class PillTopic {
        public String title;
        public String topic;
        public String priority;
        public String description;
    }
}
