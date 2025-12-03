package com.tala.ai.controller;

import com.tala.ai.client.OriginDataServiceFeignClient;
import com.tala.ai.dto.EventExtractionResult;
import com.tala.ai.service.AIProcessingOrchestrator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Processing Controller
 * 
 * Provides endpoints for the 3-stage AI processing pipeline
 */
@RestController
@RequestMapping("/api/v1/ai/processing")
@RequiredArgsConstructor
@Slf4j
public class AIProcessingController {
    
    private final AIProcessingOrchestrator orchestrator;
    private final OriginDataServiceFeignClient originDataServiceFeignClient;
    
    /**
     * Process user input through complete AI pipeline
     * 
     * POST /api/v1/ai/processing/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<AIProcessingOrchestrator.ProcessingResult> analyzeInput(
            @RequestBody ProcessingRequest request) {
        
        log.info("POST /api/v1/ai/processing/analyze - profileId: {}, message length: {}", 
                request.profileId,
                request.userMessage != null ? request.userMessage.length() : 0);
        
        // Convert request to orchestrator format
        AIProcessingOrchestrator.ProcessingRequest orchRequest = new AIProcessingOrchestrator.ProcessingRequest();
        orchRequest.userMessage = request.userMessage;
        orchRequest.attachmentUrls = request.attachmentUrls;
        orchRequest.babyProfileContext = request.babyProfileContext;
        orchRequest.chatHistory = request.chatHistory;
        orchRequest.userLocalTime = request.userLocalTime;
        orchRequest.profileId = request.profileId;
        orchRequest.userId = request.userId != null ? request.userId : 1L;
        
        // Process through pipeline
        AIProcessingOrchestrator.ProcessingResult result = orchestrator.processInput(orchRequest);
        
        if (result.success) {
            // Send to origin-data-service if we have events
            if (result.eventExtractionResult != null && 
                result.eventExtractionResult.getEvents() != null &&
                !result.eventExtractionResult.getEvents().isEmpty()) {
                
                try {
                    sendToOriginDataService(request.profileId, request.userMessage, result);
                } catch (Exception e) {
                    log.error("Failed to send to origin-data-service, but AI processing succeeded", e);
                    // Don't fail the request, just log the error
                }
            }
            
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * Send processed events to origin-data-service
     */
    private void sendToOriginDataService(Long profileId, String userMessage, 
                                         AIProcessingOrchestrator.ProcessingResult result) throws Exception {
        EventExtractionResult extraction = result.eventExtractionResult;
        
        // Build ChatEventRequest for origin-data-service
        Map<String, Object> chatEventRequest = new HashMap<>();
        chatEventRequest.put("profileId", profileId);
        chatEventRequest.put("userMessage", userMessage);
        chatEventRequest.put("aiMessage", extraction.getAiMessage());
        
        // Convert extracted events
        List<Map<String, Object>> events = extraction.getEvents().stream()
                .map(event -> {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("eventCategory", event.getEventCategory());
                    eventMap.put("eventType", event.getEventType());
                    eventMap.put("timestamp", event.getTimestamp());
                    eventMap.put("summary", event.getSummary());
                    eventMap.put("eventData", event.getEventData());
                    eventMap.put("confidence", event.getConfidence());
                    return eventMap;
                })
                .collect(Collectors.toList());
        
        chatEventRequest.put("events", events);
        
        // Send to origin-data-service
        OriginDataServiceFeignClient.ChatEventResponse response = originDataServiceFeignClient.sendChatEvent(chatEventRequest);
        log.info("Successfully sent {} events to origin-data-service", events.size());
        log.debug("Origin-data-service response: {}", response.message);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Processing Service is running");
    }
    
    /**
     * Request DTO
     */
    @Data
    public static class ProcessingRequest {
        private Long profileId;  // Baby profile ID (required)
        private Long userId;     // User ID (optional, defaults to 1)
        private String userMessage;
        private List<String> attachmentUrls;
        private String babyProfileContext;
        private String chatHistory;
        private String userLocalTime;
    }
}
