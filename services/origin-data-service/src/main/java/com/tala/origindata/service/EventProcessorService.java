package com.tala.origindata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.origindata.constant.DataSourceType;
import com.tala.origindata.constant.HomeEventType;
import com.tala.origindata.constant.TimelineEventType;
import com.tala.origindata.domain.HomeEvent;
import com.tala.origindata.domain.OriginalEvent;
import com.tala.origindata.domain.TimelineEntry;
import com.tala.origindata.dto.ChatEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Event Processor Service
 * 
 * Processes OriginalEvents and converts them to HomeEvents and Timeline entries.
 * This is the core of the event processing workflow:
 * OriginalEvent → HomeEvent → TimelineEntry
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessorService {
    
    private final HomeEventService homeEventService;
    private final TimelineService timelineService;
    private final OriginalEventService originalEventService;
    private final ObjectMapper objectMapper;
    
    /**
     * Process an OriginalEvent and create corresponding HomeEvents and Timeline entries
     * 
     * @param originalEvent The original event to process
     * @return List of created timeline entry IDs
     */
    @Transactional
    public List<Long> processOriginalEvent(OriginalEvent originalEvent) {
        log.info("Processing original event: id={}, sourceType={}", 
                originalEvent.getId(), originalEvent.getSourceType());
        
        List<Long> timelineIds = new ArrayList<>();
        
        try {
            // Parse the raw payload based on source type
            // HOME_EVENT, DAY_CARE_REPORT, INCIDENT_REPORT, HEALTH_REPORT are all processed as AI chat events
            if (originalEvent.getSourceType() == DataSourceType.AI_CHAT ||
                originalEvent.getSourceType() == DataSourceType.HOME_EVENT ||
                originalEvent.getSourceType() == DataSourceType.DAY_CARE_REPORT ||
                originalEvent.getSourceType() == DataSourceType.INCIDENT_REPORT ||
                originalEvent.getSourceType() == DataSourceType.HEALTH_REPORT) {
                timelineIds = processAiChatEvent(originalEvent);
            } else {
                log.warn("Unsupported source type for processing: {}", originalEvent.getSourceType());
            }
            
            // Mark original event as processed
            originalEventService.markAsProcessed(originalEvent.getId());
            
            log.info("Completed processing original event: id={}, created {} timeline entries", 
                    originalEvent.getId(), timelineIds.size());
            
        } catch (Exception e) {
            log.error("Failed to process original event: id={}", originalEvent.getId(), e);
            throw new RuntimeException("Failed to process original event: " + originalEvent.getId(), e);
        }
        
        return timelineIds;
    }
    
    /**
     * Process AI Chat event
     * Data model: 1 OriginalEvent -> 1 HomeEvent -> N TimelineEntries
     */
    private List<Long> processAiChatEvent(OriginalEvent originalEvent) throws Exception {
        List<Long> timelineIds = new ArrayList<>();
        
        // Parse JSON payload
        ChatEventRequest chatRequest = objectMapper.readValue(
                originalEvent.getRawPayload(), 
                ChatEventRequest.class);
        
        if (chatRequest.getEvents() == null || chatRequest.getEvents().isEmpty()) {
            log.info("No extracted events in chat request, skipping HomeEvent/Timeline creation");
            return timelineIds;
        }
        
        // Create ONE HomeEvent for the entire chat message
        HomeEvent homeEvent = createHomeEventFromChatRequest(
                originalEvent, chatRequest);
        
        log.info("Created HomeEvent (id={}) for chat message with {} extracted events", 
                homeEvent.getId(), chatRequest.getEvents().size());
        
        // Create N TimelineEntries, one for each extracted event
        for (ChatEventRequest.ExtractedEvent extractedEvent : chatRequest.getEvents()) {
            try {
                TimelineEntry timeline = createTimelineEntryFromExtracted(
                        originalEvent, homeEvent, chatRequest, extractedEvent);
                
                timelineIds.add(timeline.getId());
                
                log.info("Created Timeline (id={}) for event type: {}", 
                        timeline.getId(), extractedEvent.getEventType());
                
            } catch (Exception e) {
                log.error("Failed to create timeline entry for event type: {}", 
                        extractedEvent.getEventType(), e);
                // Continue processing other events
            }
        }
        
        return timelineIds;
    }
    
    /**
     * Create ONE HomeEvent from the entire chat request
     * This represents the parent's single message that may contain multiple events
     */
    private HomeEvent createHomeEventFromChatRequest(
            OriginalEvent originalEvent,
            ChatEventRequest chatRequest) throws Exception {
        
        // Use the first event's type as the primary type, or default to NOTES if multiple types
        ChatEventRequest.ExtractedEvent firstEvent = chatRequest.getEvents().get(0);
        HomeEventType homeEventType = chatRequest.getEvents().size() == 1
                ? mapToHomeEventType(firstEvent.getEventType())
                : HomeEventType.NOTES; // Multiple events -> treat as general note
        
        // Use first event's timestamp or original event time
        Instant eventTime = firstEvent.getTimestamp() != null
                ? firstEvent.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
                : originalEvent.getEventTime();
        
        // Create a summary title
        String title = chatRequest.getEvents().size() == 1
                ? firstEvent.getSummary()
                : String.format("%d events recorded", chatRequest.getEvents().size());
        
        // Use AI message as description
        String description = chatRequest.getAiMessage();
        
        // Store all events as JSON in details
        String detailsJson = objectMapper.writeValueAsString(chatRequest.getEvents());
        
        // Extract location from first event
        String location = extractLocationFromEventData(firstEvent.getEventData());
        
        // Build HomeEvent
        HomeEvent homeEvent = HomeEvent.builder()
                .originalEventId(originalEvent.getId())
                .profileId(originalEvent.getProfileId())
                .eventType(homeEventType)
                .eventTime(eventTime)
                .title(title)
                .description(description)
                .details(detailsJson)
                .location(location)
                .build();
        
        return homeEventService.createHomeEvent(homeEvent);
    }
    
    /**
     * Create TimelineEntry from extracted event data
     */
    private TimelineEntry createTimelineEntryFromExtracted(
            OriginalEvent originalEvent,
            HomeEvent homeEvent,
            ChatEventRequest chatRequest,
            ChatEventRequest.ExtractedEvent extractedEvent) throws Exception {
        
        // Map to TimelineEventType
        TimelineEventType timelineType = mapToTimelineEventType(extractedEvent.getEventType());
        
        // Extract AI tags from event_data
        String aiTagsJson = "[]";
        if (extractedEvent.getEventData() != null && extractedEvent.getEventData().containsKey("ai_tags")) {
            Object aiTagsObj = extractedEvent.getEventData().get("ai_tags");
            log.debug("AI tags object type: {}, value: {}", 
                    aiTagsObj != null ? aiTagsObj.getClass().getName() : "null", aiTagsObj);
            if (aiTagsObj instanceof List || aiTagsObj instanceof String) {
                aiTagsJson = objectMapper.writeValueAsString(aiTagsObj);
                log.info("Extracted AI tags JSON: {}", aiTagsJson);
            }
        } else {
            log.warn("No ai_tags found in eventData. EventData keys: {}", 
                    extractedEvent.getEventData() != null ? extractedEvent.getEventData().keySet() : "null");
        }
        
        // Determine timeline-specific timestamp
        Instant timelineRecordTime = extractedEvent.getTimestamp() != null
                ? extractedEvent.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
                : homeEvent.getEventTime();
        
        // Build TimelineEntry
        TimelineEntry timeline = TimelineEntry.builder()
                .originalEventId(originalEvent.getId())
                .profileId(originalEvent.getProfileId())
                .timelineType(timelineType)
                .dataSource(originalEvent.getSourceType())
                .recordTime(timelineRecordTime)
                .title(extractedEvent.getSummary())  // Use individual event summary as title
                .aiSummary(extractedEvent.getSummary())
                .aiTags(aiTagsJson)
                .location(extractLocationFromEventData(extractedEvent.getEventData()))
                .aiModelVersion("gemini-2.5-flash") // TODO: Extract from event metadata
                .originalUserMessage(chatRequest.getUserMessage())
                .build();
        
        return timelineService.createTimelineEntry(timeline);
    }
    
    /**
     * Map event type string to HomeEventType enum
     */
    private HomeEventType mapToHomeEventType(String eventType) {
        if (eventType == null) {
            return HomeEventType.NOTES;
        }
        
        try {
            return HomeEventType.valueOf(eventType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown event type: {}, defaulting to NOTES", eventType);
            return HomeEventType.NOTES;
        }
    }
    
    /**
     * Map event type string to TimelineEventType enum
     */
    private TimelineEventType mapToTimelineEventType(String eventType) {
        if (eventType == null) {
            return TimelineEventType.NOTES;
        }
        
        // Handle special mappings
        String normalized = eventType.toUpperCase();
        if ("DIAPER".equals(normalized)) {
            return TimelineEventType.DIAPER_CHANGE;
        }
        
        try {
            return TimelineEventType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown timeline event type: {}, defaulting to NOTES", eventType);
            return TimelineEventType.NOTES;
        }
    }
    
    /**
     * Extract location from event data if present
     */
    private String extractLocationFromEventData(Map<String, Object> eventData) {
        if (eventData == null) {
            return "Home";
        }
        
        Object location = eventData.get("location");
        return location != null ? location.toString() : "Home";
    }
    
    /**
     * Process all unprocessed OriginalEvents
     * This can be called by a scheduled job or on-demand
     */
    @Transactional
    public void processAllUnprocessedEvents() {
        List<OriginalEvent> unprocessedEvents = originalEventService.getUnprocessedEvents();
        
        log.info("Found {} unprocessed events to process", unprocessedEvents.size());
        
        for (OriginalEvent event : unprocessedEvents) {
            try {
                processOriginalEvent(event);
            } catch (Exception e) {
                log.error("Failed to process event: id={}", event.getId(), e);
                // Continue processing other events
            }
        }
        
        log.info("Completed processing batch of {} events", unprocessedEvents.size());
    }
}
