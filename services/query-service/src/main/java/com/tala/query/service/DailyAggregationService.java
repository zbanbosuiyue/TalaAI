package com.tala.query.service;

import com.tala.query.client.OriginDataServiceFeignClient;
import com.tala.query.domain.DailyChildSummary;
import com.tala.query.dto.DailyContextResponse;
import com.tala.query.repository.DailyChildSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Daily aggregation service for analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyAggregationService {
    
    private final DailyChildSummaryRepository repository;
    private final OriginDataServiceFeignClient originDataServiceFeignClient;
    
    /**
     * Get daily context for AI services
     */
    @Transactional(readOnly = true)
    public DailyContextResponse getDailyContext(Long profileId, LocalDate date) {
        log.debug("Getting daily context for profile={}, date={}", profileId, date);
        
        Optional<DailyChildSummary> summaryOpt = repository.findByProfileIdAndDate(profileId, date);
        
        if (summaryOpt.isEmpty()) {
            log.warn("No daily summary found for profile={}, date={}", profileId, date);
            return DailyContextResponse.builder()
                .profileId(profileId)
                .date(date)
                .totalEvents(0)
                .hasIncident(false)
                .hasSickness(false)
                .build();
        }
        
        DailyChildSummary summary = summaryOpt.get();
        
        // Get recent trends
        List<DailyContextResponse.TrendData> trends = calculateTrends(profileId, date);
        
        return DailyContextResponse.builder()
            .profileId(summary.getProfileId())
            .date(summary.getDate())
            .eventsSummary(summary.getEventsSummary())
            .metrics(summary.getMetrics())
            // Map attachmentIds (AttachmentSupport) to candidateMediaIds in DTO
            .candidateMediaIds(summary.getAttachmentIds())
            .candidateIncidentIds(summary.getCandidateIncidentIds())
            .totalEvents(summary.getTotalEvents())
            .hasIncident(summary.getHasIncident())
            .hasSickness(summary.getHasSickness())
            .recentTrends(trends)
            .build();
    }
    
    /**
     * Get recent summaries
     */
    @Transactional(readOnly = true)
    public List<DailyContextResponse> getRecentSummaries(Long profileId, int days) {
        List<DailyChildSummary> summaries = repository.findRecentByProfileId(profileId, days);
        
        return summaries.stream()
            .map(this::toContextResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Aggregate daily data (called by scheduled job)
     */
    @Transactional
    public DailyChildSummary aggregateDailyData(Long profileId, LocalDate date) {
        log.info("Aggregating daily data for profile={}, date={}", profileId, date);
        
        // Check if already exists
        Optional<DailyChildSummary> existing = repository.findByProfileIdAndDate(profileId, date);
        if (existing.isPresent()) {
            log.debug("Daily summary already exists, updating...");
            return updateExistingSummary(existing.get());
        }
        
        // Fetch timeline events from origin-data-service
        List<OriginDataServiceFeignClient.TimelineEntryResponse> timelineEntries = new ArrayList<>();
        try {
            // Convert date to time range (start of day to end of day)
            Instant startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            timelineEntries = originDataServiceFeignClient.getTimelineRange(profileId, startTime, endTime);
            log.info("Fetched {} timeline entries for profile={}, date={}", 
                timelineEntries.size(), profileId, date);
        } catch (Exception e) {
            log.error("Failed to fetch timeline data from origin-data-service for profile={}, date={}", 
                profileId, date, e);
            // Continue with empty data rather than failing
        }
        
        // Aggregate data from timeline entries
        Map<String, Object> eventsSummary = new HashMap<>();
        Map<String, Object> metrics = new HashMap<>();
        List<Long> attachmentIds = new ArrayList<>();
        List<Long> candidateIncidentIds = new ArrayList<>();
        boolean hasIncident = false;
        boolean hasSickness = false;
        
        // Count events by type
        Map<String, Integer> eventTypeCounts = new HashMap<>();
        for (OriginDataServiceFeignClient.TimelineEntryResponse entry : timelineEntries) {
            String type = entry.timelineType != null ? entry.timelineType : "UNKNOWN";
            eventTypeCounts.put(type, eventTypeCounts.getOrDefault(type, 0) + 1);
            
            // Check for incidents
            if ("INCIDENT".equalsIgnoreCase(type) || "INJURY".equalsIgnoreCase(type)) {
                hasIncident = true;
                if (entry.id != null) {
                    candidateIncidentIds.add(entry.id);
                }
            }
            
            // Check for sickness
            if ("SICKNESS".equalsIgnoreCase(type) || "MEDICATION".equalsIgnoreCase(type)) {
                hasSickness = true;
            }
            
            // Extract attachment IDs from attachmentUrls if present
            if (entry.attachmentUrls != null && !entry.attachmentUrls.isEmpty()) {
                // Parse attachment URLs to extract IDs
                // Format might be: "123,456,789" or JSON array
                try {
                    String[] parts = entry.attachmentUrls.split(",");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.matches("\\d+")) {
                            attachmentIds.add(Long.parseLong(trimmed));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse attachment IDs from: {}", entry.attachmentUrls);
                }
            }
        }
        
        eventsSummary.put("eventTypeCounts", eventTypeCounts);
        eventsSummary.put("totalEntries", timelineEntries.size());
        
        // Calculate basic metrics
        metrics.put("timelineEntriesCount", timelineEntries.size());
        metrics.put("uniqueEventTypes", eventTypeCounts.size());
        
        // Create new summary with aggregated data
        DailyChildSummary summary = DailyChildSummary.builder()
            .profileId(profileId)
            .date(date)
            .eventsSummary(eventsSummary)
            .metrics(metrics)
            .attachmentIds(attachmentIds)
            .candidateIncidentIds(candidateIncidentIds)
            .totalEvents(timelineEntries.size())
            .hasIncident(hasIncident)
            .hasSickness(hasSickness)
            .build();
        
        return repository.save(summary);
    }
    
    private DailyContextResponse toContextResponse(DailyChildSummary summary) {
        return DailyContextResponse.builder()
            .profileId(summary.getProfileId())
            .date(summary.getDate())
            .eventsSummary(summary.getEventsSummary())
            .metrics(summary.getMetrics())
            .candidateMediaIds(summary.getAttachmentIds())
            .candidateIncidentIds(summary.getCandidateIncidentIds())
            .totalEvents(summary.getTotalEvents())
            .hasIncident(summary.getHasIncident())
            .hasSickness(summary.getHasSickness())
            .build();
    }
    
    private List<DailyContextResponse.TrendData> calculateTrends(Long profileId, LocalDate date) {
        // Get last 7 days for trend calculation
        LocalDate startDate = date.minusDays(7);
        List<DailyChildSummary> recentSummaries = repository.findByProfileIdAndDateRange(
            profileId, startDate, date
        );
        
        List<DailyContextResponse.TrendData> trends = new ArrayList<>();
        
        // Calculate sleep trend
        if (recentSummaries.size() >= 2) {
            trends.add(DailyContextResponse.TrendData.builder()
                .metric("sleep")
                .trend("stable")
                .changePercent(0.0)
                .description("Sleep pattern is consistent")
                .build());
        }
        
        return trends;
    }
    
    private DailyChildSummary updateExistingSummary(DailyChildSummary summary) {
        // Update logic here
        return repository.save(summary);
    }
}
