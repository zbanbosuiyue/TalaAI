package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for Media Service
 */
@FeignClient(name = "media-service", url = "${feign.services.media-service.url}")
public interface MediaServiceClient {
    
    @GetMapping("/api/v1/media")
    List<MediaResponse> getMediaByDate(
        @RequestParam Long profileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
    
    class MediaResponse {
        public Long id;
        public Long profileId;
        public String source;
        public String mediaType;
        public List<String> aiTags;
        public Integer emotionScore;
    }
}
