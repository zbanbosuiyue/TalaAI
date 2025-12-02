package com.tala.query.service;

import com.tala.query.domain.DailyChildSummary;
import com.tala.query.dto.DailyContextResponse;
import com.tala.query.repository.DailyChildSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        
        // Create new summary
        DailyChildSummary summary = DailyChildSummary.builder()
            .profileId(profileId)
            .date(date)
            .eventsSummary(new HashMap<>())
            .metrics(new HashMap<>())
            // Use attachmentIds field defined by AttachmentSupport
            .attachmentIds(new ArrayList<>())
            .candidateIncidentIds(new ArrayList<>())
            .totalEvents(0)
            .hasIncident(false)
            .hasSickness(false)
            .build();
        
        // TODO: Fetch and aggregate events from origin-data-service timeline
        // This should call origin-data-service APIs or read from shared database views
        
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
