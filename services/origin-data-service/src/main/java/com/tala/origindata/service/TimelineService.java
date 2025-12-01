package com.tala.origindata.service;

import com.tala.core.dto.AttachmentRef;
import com.tala.origindata.constant.TimelineEventType;
import com.tala.origindata.domain.OriginalEvent;
import com.tala.origindata.domain.TimelineEntry;
import com.tala.origindata.dto.TimelineEntryResponse;
import com.tala.origindata.repository.OriginalEventRepository;
import com.tala.origindata.repository.TimelineEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Timeline Entries (AI-generated display data)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {
    
    private final TimelineEntryRepository timelineEntryRepository;
    private final OriginalEventRepository originalEventRepository;
    private final AttachmentResolverService attachmentResolverService;
    
    /**
     * Get timeline for profile (paginated)
     */
    @Transactional(readOnly = true)
    public Page<TimelineEntry> getTimelineByProfile(Long profileId, Pageable pageable) {
        return timelineEntryRepository.findByProfileIdOrderByRecordTimeDesc(profileId, pageable);
    }
    
    /**
     * Get timeline by profile and type
     */
    @Transactional(readOnly = true)
    public Page<TimelineEntry> getTimelineByProfileAndType(
            Long profileId, TimelineEventType eventType, Pageable pageable) {
        return timelineEntryRepository.findByProfileIdAndTimelineTypeOrderByRecordTimeDesc(
                profileId, eventType, pageable);
    }
    
    /**
     * Get timeline by profile and time range
     */
    @Transactional(readOnly = true)
    public List<TimelineEntry> getTimelineByProfileAndTimeRange(
            Long profileId, Instant startTime, Instant endTime) {
        return timelineEntryRepository.findByProfileIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                profileId, startTime, endTime);
    }
    
    /**
     * Get timeline entry by ID
     */
    @Transactional(readOnly = true)
    public Optional<TimelineEntry> getTimelineEntryById(Long id) {
        return timelineEntryRepository.findById(id);
    }
    
    /**
     * Get timeline entries by original event
     */
    @Transactional(readOnly = true)
    public List<TimelineEntry> getTimelineEntriesByOriginalEvent(Long originalEventId) {
        return timelineEntryRepository.findByOriginalEventId(originalEventId);
    }
    
    /**
     * Create timeline entry
     */
    @Transactional
    public TimelineEntry createTimelineEntry(TimelineEntry entry) {
        TimelineEntry saved = timelineEntryRepository.save(entry);
        log.info("Created timeline entry: id={}, profileId={}, type={}", 
                saved.getId(), saved.getProfileId(), saved.getTimelineType());
        return saved;
    }
    
    /**
     * Count timeline entries by profile
     */
    @Transactional(readOnly = true)
    public long countByProfile(Long profileId) {
        return timelineEntryRepository.countByProfileId(profileId);
    }
    
    /**
     * Count timeline entries by profile and type
     */
    @Transactional(readOnly = true)
    public long countByProfileAndType(Long profileId, TimelineEventType eventType) {
        return timelineEntryRepository.countByProfileIdAndTimelineType(profileId, eventType);
    }
    
    /**
     * Convert TimelineEntry to TimelineEntryResponse with attachments
     */
    public TimelineEntryResponse toResponse(TimelineEntry entry) {
        List<AttachmentRef> attachments = List.of();
        
        // Resolve attachments from OriginalEvent
        Optional<OriginalEvent> originalEvent = originalEventRepository.findById(entry.getOriginalEventId());
        if (originalEvent.isPresent() && originalEvent.get().hasAttachments()) {
            attachments = attachmentResolverService.resolveAttachments(
                originalEvent.get().getAttachmentIds()
            );
        }
        
        return TimelineEntryResponse.builder()
            .id(entry.getId())
            .originalEventId(entry.getOriginalEventId())
            .profileId(entry.getProfileId())
            .timelineType(entry.getTimelineType() != null ? entry.getTimelineType().name() : null)
            .dataSource(entry.getDataSource() != null ? entry.getDataSource().name() : null)
            .recordTime(entry.getRecordTime())
            .title(entry.getTitle())
            .aiSummary(entry.getAiSummary())
            .aiTags(entry.getAiTags())
            .location(entry.getLocation())
            .aiModelVersion(entry.getAiModelVersion())
            .attachments(attachments)
            .createdAt(entry.getCreatedAt())
            .updatedAt(entry.getUpdatedAt())
            .build();
    }
    
    /**
     * Convert list of TimelineEntry to TimelineEntryResponse
     */
    public List<TimelineEntryResponse> toResponseList(List<TimelineEntry> entries) {
        return entries.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert Page of TimelineEntry to Page of TimelineEntryResponse
     */
    public Page<TimelineEntryResponse> toResponsePage(Page<TimelineEntry> page) {
        return page.map(this::toResponse);
    }
}
