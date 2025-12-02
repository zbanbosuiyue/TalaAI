package com.tala.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.client.OriginDataServiceClient;
import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import com.tala.ai.dto.EventExtractionResult;
import com.tala.ai.service.AIProcessingOrchestrator;
import com.tala.ai.service.ChatMessageService;
import com.tala.ai.service.Mem0Service;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Chat Controller with SSE support
 * 
 * Handles real-world chat workflow:
 * 1. Frontend sends chat message (text + optional attachments)
 * 2. Backend streams AI thinking process via SSE
 * 3. AI processes: classification → extraction → formatting
 * 4. Send formatted events to origin-data-service
 * 5. Return final response to frontend
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final AIProcessingOrchestrator orchestrator;
    private final OriginDataServiceClient originDataServiceClient;
    private final Mem0Service mem0Service;
    private final ChatMessageService chatMessageService;
    
    /**
     * Chat with AI (streaming via SSE)
     * 
     * POST /api/v1/chat/stream
     * 
     * Real-world workflow:
     * - User sends message: "Baby drank 120ml formula at 2pm"
     * - AI streams thinking process
     * - Extracts structured event data
     * - Sends to origin-data-service
     * - Returns confirmation
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        log.info("POST /api/v1/chat/stream - profileId: {}, userId: {}, message: {}", 
                request.profileId, request.userId, 
                request.message != null ? request.message.substring(0, Math.min(50, request.message.length())) : "null");
        
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(5));
        
        emitter.onCompletion(() -> log.debug("SSE chat stream completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE chat stream timeout");
            emitter.complete();
        });
        emitter.onError((e) -> {
            log.error("SSE chat stream error", e);
            emitter.completeWithError(e);
        });
        
        // Store user message first
        try {
            chatMessageService.storeUserMessage(request.profileId, request.userId, request.message);
        } catch (Exception e) {
            log.warn("Failed to store user message, continuing with processing", e);
        }
        
        // Process chat asynchronously
        CompletableFuture.runAsync(() -> processChatWithStreaming(request, emitter));
        
        return emitter;
    }
    
    /**
     * Process chat with streaming updates
     */
    private void processChatWithStreaming(ChatRequest request, SseEmitter emitter) {
        try {
            // Step 1: Send initial thinking event
            sendEvent(emitter, "thinking", Map.of(
                    "stage", "initialization",
                    "message", "Processing your message..."
            ));
            
            // Step 2: Retrieve chat history from mem0 (if enabled)
            String chatHistory = null;
            String memoryContext = null;
            if (request.userId != null && request.profileId != null) {
                try {
                    sendEvent(emitter, "thinking", Map.of(
                            "stage", "memory_retrieval",
                            "message", "Retrieving conversation context..."
                    ));
                    
                    chatHistory = mem0Service.getChatHistory(request.userId, request.profileId);
                    memoryContext = mem0Service.getRelevantMemories(request.message, request.userId, request.profileId);
                    
                    log.info("Retrieved chat history length: {}, memory context length: {}", 
                            chatHistory != null ? chatHistory.length() : 0,
                            memoryContext != null ? memoryContext.length() : 0);
                } catch (Exception e) {
                    log.warn("Failed to retrieve mem0 data, continuing without it", e);
                }
            }
            
            // Step 3: Build baby profile context
            String babyProfileContext = buildBabyProfileContext(request.profileId);
            
            // Step 4: Build processing request
            sendEvent(emitter, "thinking", Map.of(
                    "stage", "ai_processing",
                    "message", "Analyzing your message with AI..."
            ));
            
            AIProcessingOrchestrator.ProcessingRequest orchRequest = new AIProcessingOrchestrator.ProcessingRequest();
            orchRequest.userMessage = request.message;
            orchRequest.attachmentUrls = request.attachmentUrls;
            orchRequest.babyProfileContext = babyProfileContext;
            orchRequest.chatHistory = combineHistoryAndMemory(chatHistory, memoryContext);
            orchRequest.userLocalTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            orchRequest.profileId = request.profileId;
            orchRequest.userId = request.userId;
            
            // Step 5: Process through AI pipeline
            AIProcessingOrchestrator.ProcessingResult result = orchestrator.processInput(orchRequest);
            
            // Step 6: Send classification result
            if (result.chatClassificationResult != null) {
                sendEvent(emitter, "classification", Map.of(
                        "interactionType", result.chatClassificationResult.getInteractionType(),
                        "confidence", result.chatClassificationResult.getConfidence(),
                        "reason", result.chatClassificationResult.getClassificationReason()
                ));
            }
            
            // Step 7: Send extraction result
            if (result.eventExtractionResult != null) {
                EventExtractionResult extraction = result.eventExtractionResult;
                
                sendEvent(emitter, "extraction", Map.of(
                        "aiMessage", extraction.getAiMessage(),
                        "intentUnderstanding", extraction.getIntentUnderstanding(),
                        "confidence", extraction.getConfidence(),
                        "eventsCount", extraction.getEvents() != null ? extraction.getEvents().size() : 0
                ));
                
                // Step 8: Send individual events
                if (extraction.getEvents() != null && !extraction.getEvents().isEmpty()) {
                    for (EventExtractionResult.ExtractedEvent event : extraction.getEvents()) {
                        sendEvent(emitter, "event", Map.of(
                                "eventType", event.getEventType(),
                                "eventCategory", event.getEventCategory(),
                                "timestamp", event.getTimestamp(),
                                "summary", event.getSummary(),
                                "confidence", event.getConfidence(),
                                "eventData", event.getEventData()
                        ));
                    }
                    
                    // Step 9: Send to origin-data-service
                    sendEvent(emitter, "thinking", Map.of(
                            "stage", "storing_events",
                            "message", "Saving events to database..."
                    ));
                    
                    try {
                        String originDataResponse = sendToOriginDataService(request, result);
                        
                        sendEvent(emitter, "storage", Map.of(
                                "success", true,
                                "message", "Events stored successfully",
                                "eventsCount", extraction.getEvents().size()
                        ));
                    } catch (Exception e) {
                        log.error("Failed to send to origin-data-service", e);
                        sendEvent(emitter, "storage", Map.of(
                                "success", false,
                                "error", e.getMessage()
                        ));
                    }
                }
                
                // Step 10: Store assistant message in database
                try {
                    String thinkingProcess = buildThinkingProcess(result);
                    chatMessageService.storeAssistantMessage(
                            request.profileId,
                            request.userId,
                            extraction.getAiMessage(),
                            result.chatClassificationResult != null ? 
                                    result.chatClassificationResult.getInteractionType().toString() : null,
                            extraction.getConfidence(),
                            extraction,
                            thinkingProcess
                    );
                } catch (Exception e) {
                    log.warn("Failed to store assistant message", e);
                }
                
                // Step 11: Store conversation in mem0
                if (request.userId != null && request.profileId != null) {
                    try {
                        sendEvent(emitter, "thinking", Map.of(
                                "stage", "memory_storage",
                                "message", "Updating conversation memory..."
                        ));
                        
                        mem0Service.addMessage(request.userId, request.profileId, "user", request.message);
                        mem0Service.addMessage(request.userId, request.profileId, "assistant", extraction.getAiMessage());
                        
                    } catch (Exception e) {
                        log.warn("Failed to store in mem0", e);
                    }
                }
            }
            
            // Step 12: Send final response
            sendEvent(emitter, "complete", Map.of(
                    "success", result.success,
                    "message", result.eventExtractionResult != null ? 
                            result.eventExtractionResult.getAiMessage() : "Processing complete"
            ));
            
            emitter.complete();
            
        } catch (Exception e) {
            log.error("Error processing chat", e);
            try {
                sendEvent(emitter, "error", Map.of(
                        "message", "Failed to process chat: " + e.getMessage()
                ));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("Failed to send error event", ioException);
            }
        }
    }
    
    /**
     * Send SSE event
     */
    private void sendEvent(SseEmitter emitter, String eventType, Map<String, Object> data) throws IOException {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventType)
                .data(data);
        emitter.send(event);
        
        // Small delay to ensure events are received in order
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Send processed events to origin-data-service
     */
    private String sendToOriginDataService(ChatRequest request, 
                                          AIProcessingOrchestrator.ProcessingResult result) throws Exception {
        EventExtractionResult extraction = result.eventExtractionResult;
        
        // Validate required field
        if (request.profileId == null) {
            throw new TalaException(ErrorCode.BAD_REQUEST, 
                    "profileId is required in chat request");
        }
        
        // Build ChatEventRequest for origin-data-service
        Map<String, Object> chatEventRequest = new HashMap<>();
        chatEventRequest.put("profileId", request.profileId);
        chatEventRequest.put("userMessage", request.message);
        chatEventRequest.put("aiMessage", extraction.getAiMessage());
        chatEventRequest.put("attachmentUrls", request.attachmentUrls);
        
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
        String response = originDataServiceClient.sendChatEvent(chatEventRequest);
        log.info("Successfully sent {} events to origin-data-service", events.size());
        
        return response;
    }
    
    /**
     * Build baby profile context
     */
    private String buildBabyProfileContext(Long profileId) {
        // TODO: Fetch actual profile data from user-service
        return String.format("""
                Baby Profile ID: %d
                Name: Emma
                Age: 6 months
                Birth Date: 2024-06-01
                """, profileId);
    }
    
    /**
     * Combine chat history and memory context
     */
    private String combineHistoryAndMemory(String chatHistory, String memoryContext) {
        StringBuilder combined = new StringBuilder();
        
        if (memoryContext != null && !memoryContext.isBlank()) {
            combined.append("=== RELEVANT MEMORIES ===\n");
            combined.append(memoryContext).append("\n\n");
        }
        
        if (chatHistory != null && !chatHistory.isBlank()) {
            combined.append("=== RECENT CHAT HISTORY ===\n");
            combined.append(chatHistory).append("\n");
        }
        
        return combined.length() > 0 ? combined.toString() : null;
    }
    
    /**
     * Build thinking process JSON for storage
     */
    private String buildThinkingProcess(AIProcessingOrchestrator.ProcessingResult result) {
        try {
            Map<String, Object> thinking = new HashMap<>();
            
            if (result.attachmentParserResult != null) {
                thinking.put("attachmentParsing", Map.of(
                        "summary", result.attachmentParserResult.getOverallSummary(),
                        "confidence", result.attachmentParserResult.getConfidence()
                ));
            }
            
            if (result.chatClassificationResult != null) {
                thinking.put("classification", Map.of(
                        "interactionType", result.chatClassificationResult.getInteractionType(),
                        "reason", result.chatClassificationResult.getClassificationReason(),
                        "confidence", result.chatClassificationResult.getConfidence()
                ));
            }
            
            if (result.eventExtractionResult != null) {
                thinking.put("extraction", Map.of(
                        "intentUnderstanding", result.eventExtractionResult.getIntentUnderstanding(),
                        "eventsCount", result.eventExtractionResult.getEvents() != null ? 
                                result.eventExtractionResult.getEvents().size() : 0,
                        "confidence", result.eventExtractionResult.getConfidence()
                ));
            }
            
            return new ObjectMapper().writeValueAsString(thinking);
        } catch (Exception e) {
            log.warn("Failed to build thinking process", e);
            return null;
        }
    }
    
    /**
     * Chat without streaming (simple REST)
     * 
     * POST /api/v1/chat
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("POST /api/v1/chat - profileId: {}, message length: {}", 
                request.profileId, 
                request.message != null ? request.message.length() : 0);
        
        try {
            // Store user message
            try {
                chatMessageService.storeUserMessage(request.profileId, request.userId, request.message);
            } catch (Exception e) {
                log.warn("Failed to store user message", e);
            }
            
            // Build processing request
            AIProcessingOrchestrator.ProcessingRequest orchRequest = new AIProcessingOrchestrator.ProcessingRequest();
            orchRequest.userMessage = request.message;
            orchRequest.attachmentUrls = request.attachmentUrls;
            orchRequest.babyProfileContext = buildBabyProfileContext(request.profileId);
            orchRequest.chatHistory = null; // No history for simple chat
            orchRequest.userLocalTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            orchRequest.profileId = request.profileId;
            orchRequest.userId = request.userId;
            
            // Process
            AIProcessingOrchestrator.ProcessingResult result = orchestrator.processInput(orchRequest);
            
            // Send to origin-data-service if we have events
            if (result.success && result.eventExtractionResult != null && 
                result.eventExtractionResult.getEvents() != null &&
                !result.eventExtractionResult.getEvents().isEmpty()) {
                
                try {
                    sendToOriginDataService(request, result);
                } catch (Exception e) {
                    log.error("Failed to send to origin-data-service", e);
                }
            }
            
            // Store assistant message
            if (result.success && result.eventExtractionResult != null) {
                try {
                    String thinkingProcess = buildThinkingProcess(result);
                    chatMessageService.storeAssistantMessage(
                            request.profileId,
                            request.userId,
                            result.eventExtractionResult.getAiMessage(),
                            result.chatClassificationResult != null ? 
                                    result.chatClassificationResult.getInteractionType().toString() : null,
                            result.eventExtractionResult.getConfidence(),
                            result.eventExtractionResult,
                            thinkingProcess
                    );
                } catch (Exception e) {
                    log.warn("Failed to store assistant message", e);
                }
            }
            
            // Build response
            ChatResponse response = new ChatResponse();
            response.success = result.success;
            response.message = result.eventExtractionResult != null ? 
                    result.eventExtractionResult.getAiMessage() : "Processing complete";
            response.interactionType = result.chatClassificationResult != null ? 
                    result.chatClassificationResult.getInteractionType().toString() : null;
            response.eventsCount = result.eventExtractionResult != null && 
                    result.eventExtractionResult.getEvents() != null ? 
                    result.eventExtractionResult.getEvents().size() : 0;
            response.events = result.eventExtractionResult != null ? 
                    result.eventExtractionResult.getEvents() : null;
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to process chat", e);
            
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.success = false;
            errorResponse.message = "Failed to process chat: " + e.getMessage();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Controller is running");
    }
    
    /**
     * Chat Request DTO
     */
    @Data
    public static class ChatRequest {
        private Long profileId;
        private Long userId;
        private String message;
        private List<String> attachmentUrls;
    }
    
    /**
     * Chat Response DTO
     */
    @Data
    public static class ChatResponse {
        private Boolean success;
        private String message;
        private String interactionType;
        private Integer eventsCount;
        private List<EventExtractionResult.ExtractedEvent> events;
    }
}
