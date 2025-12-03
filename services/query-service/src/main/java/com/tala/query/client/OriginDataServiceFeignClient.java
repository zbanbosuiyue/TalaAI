package com.tala.query.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

/**
 * Feign Client for Origin Data Service
 * 
 * Automatically propagates JWT tokens from incoming requests to origin-data-service.
 * Uses FeignJwtConfig for JWT token propagation.
 */
@FeignClient(
    name = "origin-data-service",
    url = "${services.origin-data-service.url}",
    configuration = com.tala.core.feign.FeignJwtConfig.class
)
public interface OriginDataServiceFeignClient {
    
    /**
     * Get timeline entries for a profile within a date range
     */
    @GetMapping("/api/v1/timeline/profile/{profileId}/range")
    List<TimelineEntryResponse> getTimelineRange(
        @PathVariable("profileId") Long profileId,
        @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    );
    
    /**
     * Health check for origin-data-service
     */
    @GetMapping("/api/v1/health")
    HealthResponse health();
    
    /**
     * Timeline Entry Response DTO
     */
    class TimelineEntryResponse {
        public Long id;
        public Long profileId;
        public String timelineType;
        public String dataSource;
        public String recordTime;
        public String title;
        public String aiSummary;
        public Object aiTags;
        public String attachmentUrls;
        public String location;
        public String aiModelVersion;
        public String createdAt;
        public String updatedAt;
    }
    
    /**
     * Health Response DTO
     */
    class HealthResponse {
        public String status;
    }
}
