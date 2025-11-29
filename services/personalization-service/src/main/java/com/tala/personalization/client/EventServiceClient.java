package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for Event Service
 */
@FeignClient(name = "event-service", url = "${feign.services.event-service.url}")
public interface EventServiceClient {
    
    @GetMapping("/api/v1/events/timeline")
    List<EventResponse> getTimeline(
        @RequestParam Long profileId,
        @RequestParam Instant startTime,
        @RequestParam Instant endTime
    );
    
    class EventResponse {
        public Long id;
        public Long profileId;
        public String eventType;
        public Instant occurredAt;
        public String priority;
        public Integer urgencyHours;
        public String riskLevel;
        public String description;
    }
}
