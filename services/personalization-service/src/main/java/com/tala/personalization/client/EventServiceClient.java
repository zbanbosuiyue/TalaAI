package com.tala.personalization.client;

import com.tala.core.dto.AttachmentRef;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

/**
 * Feign client for Origin Data Service timeline
 * Automatically propagates JWT tokens for authenticated requests
 */
@FeignClient(
    name = "origin-data-service", 
    url = "${feign.services.origin-data-service.url}",
    configuration = com.tala.core.feign.FeignJwtConfig.class
)
public interface EventServiceClient {
    
    @GetMapping("/api/v1/timeline/profile/{profileId}/range")
    List<TimelineEntryResponse> getTimelineRange(
        @PathVariable("profileId") Long profileId,
        @RequestParam("startTime") Instant startTime,
        @RequestParam("endTime") Instant endTime
    );
    
    class TimelineEntryResponse {
        public Long id;
        public Long originalEventId;
        public Long profileId;
        public String timelineType;
        public String dataSource;
        public Instant recordTime;
        public String title;
        public String aiSummary;
        public String aiTags;
        public String location;
        public String aiModelVersion;
        public List<AttachmentRef> attachments;
        public Instant createdAt;
        public Instant updatedAt;
    }
}
