package com.tala.origindata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.origindata.constant.DataSourceType;
import com.tala.origindata.domain.OriginalEvent;
import com.tala.origindata.dto.ChatEventRequest;
import com.tala.origindata.service.EventProcessorService;
import com.tala.origindata.service.OriginalEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for receiving AI-processed chat events
 */
@RestController
@RequestMapping("/api/v1/chat-events")
@RequiredArgsConstructor
@Slf4j
public class ChatEventController {
    
    private final OriginalEventService originalEventService;
    private final EventProcessorService eventProcessorService;
    private final ObjectMapper objectMapper;
    
    /**
     * Create original event from AI-processed chat
     * 
     * POST /api/v1/chat-events
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createChatEvent(@RequestBody ChatEventRequest request) {
        log.info("POST /api/v1/chat-events - profileId: {}, events count: {}", 
                request.getProfileId(), 
                request.getEvents() != null ? request.getEvents().size() : 0);
        
        try {
            // Convert request to JSON payload
            String rawPayload = objectMapper.writeValueAsString(request);
            
            // Determine event time from first extracted event or use current time
            Instant eventTime = Instant.now();
            if (request.getEvents() != null && !request.getEvents().isEmpty()) {
                ChatEventRequest.ExtractedEvent firstEvent = request.getEvents().get(0);
                if (firstEvent.getTimestamp() != null) {
                    eventTime = firstEvent.getTimestamp()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant();
                }
            }
            
            // Determine data source type from AI response, fallback to AI_CHAT
            DataSourceType dataSourceType = DataSourceType.AI_CHAT;
            if (request.getDataSourceType() != null && !request.getDataSourceType().isEmpty()) {
                try {
                    dataSourceType = DataSourceType.valueOf(request.getDataSourceType());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid data source type: {}, using AI_CHAT", request.getDataSourceType());
                }
            }
            
            // Create original event with attachments (event sourcing)
            OriginalEvent originalEvent = originalEventService.createEvent(
                    request.getProfileId(),
                    dataSourceType,
                    null,  // No external source ID for chat
                    eventTime,
                    rawPayload,
                    request.getAttachmentFileIds()  // Attachment IDs from chat
            );
            
            log.info("Chat event stored: originalEventId={}, profileId={}", 
                    originalEvent.getId(), request.getProfileId());
            
            // Process the event immediately: OriginalEvent -> HomeEvent -> Timeline
            List<Long> timelineIds = List.of();
            try {
                timelineIds = eventProcessorService.processOriginalEvent(originalEvent);
                log.info("Successfully processed event: created {} timeline entries", timelineIds.size());
            } catch (Exception e) {
                log.error("Failed to process original event, but event is saved: originalEventId={}", 
                        originalEvent.getId(), e);
                // Don't fail the request - event is saved and can be reprocessed later
            }
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalEventId", originalEvent.getId());
            response.put("message", "Chat event stored successfully");
            response.put("eventsCount", request.getEvents() != null ? request.getEvents().size() : 0);
            response.put("timelineIdsCreated", timelineIds.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to store chat event", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to store chat event: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Event Controller is running");
    }
}
