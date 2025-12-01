package com.tala.origindata.service;

import com.tala.origindata.constant.DataSourceType;
import com.tala.origindata.domain.OriginalEvent;
import com.tala.origindata.repository.OriginalEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Original Events (Event Sourcing)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OriginalEventService {
    
    private final OriginalEventRepository originalEventRepository;
    
    /**
     * Create a new original event (idempotent) - without attachments
     */
    @Transactional
    public OriginalEvent createEvent(
            Long profileId,
            DataSourceType sourceType,
            String sourceEventId,
            Instant eventTime,
            String rawPayload) {
        return createEvent(profileId, sourceType, sourceEventId, eventTime, rawPayload, null);
    }
    
    /**
     * Create a new original event with attachments (idempotent)
     */
    @Transactional
    public OriginalEvent createEvent(
            Long profileId,
            DataSourceType sourceType,
            String sourceEventId,
            Instant eventTime,
            String rawPayload,
            List<Long> attachmentFileIds) {
        
        // Check for duplicate
        if (sourceEventId != null) {
            Optional<OriginalEvent> existing = originalEventRepository
                    .findBySourceTypeAndSourceEventId(sourceType, sourceEventId);
            if (existing.isPresent()) {
                log.info("Duplicate event detected: sourceType={}, sourceEventId={}", sourceType, sourceEventId);
                return existing.get();
            }
        }
        
        OriginalEvent event = OriginalEvent.builder()
                .profileId(profileId)
                .sourceType(sourceType)
                .sourceEventId(sourceEventId)
                .eventTime(eventTime)
                .rawPayload(rawPayload)
                .attachmentIds(attachmentFileIds != null ? attachmentFileIds : List.of())
                .aiProcessed(false)
                .build();
        
        OriginalEvent saved = originalEventRepository.save(event);
        log.info("Created original event: id={}, sourceType={}, profileId={}, attachments={}", 
                saved.getId(), sourceType, profileId, saved.getAttachmentCount());
        
        return saved;
    }
    
    /**
     * Mark event as AI processed
     */
    @Transactional
    public void markAsProcessed(Long eventId) {
        originalEventRepository.findById(eventId).ifPresent(event -> {
            event.setAiProcessed(true);
            event.setAiProcessedAt(Instant.now());
            originalEventRepository.save(event);
            log.info("Marked event as processed: id={}", eventId);
        });
    }
    
    /**
     * Get unprocessed events for AI processing
     */
    @Transactional(readOnly = true)
    public List<OriginalEvent> getUnprocessedEvents() {
        return originalEventRepository.findByAiProcessedFalseOrderByEventTimeAsc();
    }
    
    /**
     * Get events by profile
     */
    @Transactional(readOnly = true)
    public List<OriginalEvent> getEventsByProfile(Long profileId) {
        return originalEventRepository.findByProfileIdOrderByEventTimeDesc(profileId);
    }
    
    /**
     * Get events by profile and source type
     */
    @Transactional(readOnly = true)
    public List<OriginalEvent> getEventsByProfileAndType(Long profileId, DataSourceType sourceType) {
        return originalEventRepository.findByProfileIdAndSourceTypeOrderByEventTimeDesc(profileId, sourceType);
    }
    
    /**
     * Get events by profile and time range
     */
    @Transactional(readOnly = true)
    public List<OriginalEvent> getEventsByProfileAndTimeRange(
            Long profileId, Instant startTime, Instant endTime) {
        return originalEventRepository.findByProfileIdAndEventTimeBetweenOrderByEventTimeDesc(
                profileId, startTime, endTime);
    }
    
    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Optional<OriginalEvent> getEventById(Long id) {
        return originalEventRepository.findById(id);
    }
}
