package com.tala.personalization.controller;

import com.tala.personalization.dto.TodayPageResponse;
import com.tala.personalization.service.PersonalizationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Personalization REST API Controller
 */
@RestController
@RequestMapping("/api/v1/personalization")
@RequiredArgsConstructor
@Slf4j
public class PersonalizationController {
    
    private final PersonalizationOrchestrator orchestrator;
    
    /**
     * Get Today Menu page
     */
    @GetMapping("/today")
    @Cacheable(value = "today-page", key = "#userId + '-' + #profileId + '-' + #date")
    public ResponseEntity<TodayPageResponse> getTodayPage(
        @RequestParam Long userId,
        @RequestParam Long profileId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/v1/personalization/today - userId={}, profileId={}, date={}", 
            userId, profileId, date);
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        TodayPageResponse response = orchestrator.buildTodayPage(userId, profileId, date);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Personalization Service is running");
    }
}
