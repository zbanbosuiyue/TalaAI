package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Feign client for Query Service
 */
@FeignClient(name = "query-service", url = "${feign.services.query-service.url}")
public interface QueryServiceClient {
    
    @GetMapping("/api/v1/analytics/daily-context")
    DailyContextResponse getDailyContext(
        @RequestParam Long profileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
    
    @GetMapping("/api/v1/analytics/recent-summaries")
    List<DailyContextResponse> getRecentSummaries(
        @RequestParam Long profileId,
        @RequestParam int days
    );
    
    class DailyContextResponse {
        public Long profileId;
        public LocalDate date;
        public Map<String, Object> eventsSummary;
        public Map<String, Object> metrics;
        public List<Long> candidateMediaIds;
        public List<Long> candidateIncidentIds;
        public Integer totalEvents;
        public Boolean hasIncident;
        public Boolean hasSickness;
        public List<TrendData> recentTrends;
    }
    
    class TrendData {
        public String metric;
        public String trend;
        public Double changePercent;
        public String description;
    }
}
