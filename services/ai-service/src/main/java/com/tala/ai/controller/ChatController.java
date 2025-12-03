package com.tala.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.client.OriginDataServiceClient;
import com.tala.ai.client.UserServiceClient;
import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import com.tala.ai.dto.EventExtractionResult;
import com.tala.ai.service.AIProcessingOrchestrator;
import com.tala.ai.service.ChatMessageService;
import com.tala.ai.service.ContextEnrichmentService;
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
    private final UserServiceClient userServiceClient;
    private final Mem0Service mem0Service;
    private final ChatMessageService chatMessageService;
    private final ContextEnrichmentService contextEnrichmentService;
    
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
            
            // Step 2: Enrich user message with chat history and memory context
            String enrichedMessage = request.message;
            if (request.userId != null && request.profileId != null) {
                try {
                    sendEvent(emitter, "thinking", Map.of(
                            "stage", "context_enrichment",
                            "message", "Loading conversation history..."
                    ));
                    
                    // Use ContextEnrichmentService to properly format chat history
                    enrichedMessage = contextEnrichmentService.enrichUserMessage(
                            request.profileId, 
                            request.userId, 
                            request.message, 
                            10); // Last 10 messages
                    
                    log.info("Enriched user message with chat history and memory context");
                } catch (Exception e) {
                    log.warn("Failed to enrich message with context, using original message: {}", e.getMessage());
                    enrichedMessage = request.message;
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
            orchRequest.userMessage = enrichedMessage; // Use enriched message with history
            orchRequest.attachmentUrls = request.attachmentUrls;
            orchRequest.babyProfileContext = babyProfileContext;
            orchRequest.chatHistory = null; // History already included in enrichedMessage
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
        try {
            UserServiceClient.ProfileData profile = userServiceClient.getProfile(profileId);
            
            // Calculate age from birth date
            String ageDescription = calculateAgeDescription(profile.birthDate);
            
            StringBuilder context = new StringBuilder();
            context.append(String.format("Baby Profile ID: %d\n", profile.id));
            context.append(String.format("Name: %s\n", profile.babyName));
            context.append(String.format("Age: %s\n", ageDescription));
            context.append(String.format("Birth Date: %s\n", profile.birthDate));
            
            if (profile.gender != null && !profile.gender.isEmpty()) {
                context.append(String.format("Gender: %s\n", profile.gender));
            }
            
            if (profile.parentName != null && !profile.parentName.isEmpty()) {
                context.append(String.format("Parent: %s", profile.parentName));
                if (profile.parentRole != null && !profile.parentRole.isEmpty()) {
                    context.append(String.format(" (%s)", profile.parentRole));
                }
                context.append("\n");
            }
            
            if (profile.hasDaycare != null && profile.hasDaycare && profile.daycareName != null) {
                context.append(String.format("Daycare: %s\n", profile.daycareName));
            }
            
            if (profile.concerns != null && !profile.concerns.isEmpty()) {
                context.append(String.format("Parent Concerns: %s\n", profile.concerns));
            }
            
            return context.toString();
        } catch (Exception e) {
            log.warn("Failed to fetch profile data from user-service for profileId: {}, using fallback", profileId, e);
            // Fallback to basic context
            return String.format("""
                    Baby Profile ID: %d
                    (Profile data temporarily unavailable)
                    """, profileId);
        }
    }
    
    /**
     * Calculate age description from birth date
     */
    private String calculateAgeDescription(java.time.LocalDate birthDate) {
        if (birthDate == null) {
            return "Unknown";
        }
        
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.Period period = java.time.Period.between(birthDate, now);
        
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        
        if (years > 0) {
            if (months > 0) {
                return String.format("%d year%s %d month%s", 
                    years, years > 1 ? "s" : "", 
                    months, months > 1 ? "s" : "");
            }
            return String.format("%d year%s", years, years > 1 ? "s" : "");
        } else if (months > 0) {
            if (days > 7) {
                int weeks = days / 7;
                return String.format("%d month%s %d week%s", 
                    months, months > 1 ? "s" : "",
                    weeks, weeks > 1 ? "s" : "");
            }
            return String.format("%d month%s", months, months > 1 ? "s" : "");
        } else if (days > 0) {
            if (days >= 7) {
                int weeks = days / 7;
                return String.format("%d week%s", weeks, weeks > 1 ? "s" : "");
            }
            return String.format("%d day%s", days, days > 1 ? "s" : "");
        }
        
        return "Newborn";
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
     * Get chat history for a profile
     * 
     * GET /api/v1/chat/history?profileId={profileId}
     */
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @RequestParam Long profileId,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        log.info("GET /api/v1/chat/history - profileId: {}, limit: {}", profileId, limit);
        
        try {
            List<com.tala.ai.domain.ChatMessage> messages = chatMessageService.getAllMessages(profileId);
            
            // Convert to response DTOs
            List<ChatHistoryMessage> historyMessages = messages.stream()
                    .map(msg -> {
                        ChatHistoryMessage dto = new ChatHistoryMessage();
                        dto.id = msg.getId();
                        dto.profileId = msg.getProfileId();
                        dto.userId = msg.getUserId();
                        dto.role = msg.getRole().toString().toLowerCase();
                        dto.content = msg.getContent();
                        dto.messageType = msg.getMessageType().toString().toLowerCase();
                        dto.attachmentIds = msg.getAttachmentIds();
                        dto.attachmentCount = msg.getAttachmentCount();
                        dto.interactionType = msg.getInteractionType();
                        dto.confidence = msg.getConfidence();
                        dto.createdAt = msg.getCreatedAt();
                        dto.updatedAt = msg.getUpdatedAt();
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            ChatHistoryResponse response = new ChatHistoryResponse();
            response.success = true;
            response.messages = historyMessages;
            response.totalCount = historyMessages.size();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get chat history", e);
            
            ChatHistoryResponse errorResponse = new ChatHistoryResponse();
            errorResponse.success = false;
            errorResponse.messages = List.of();
            errorResponse.totalCount = 0;
            
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
    
    /**
     * Chat History Response DTO
     */
    @Data
    public static class ChatHistoryResponse {
        private Boolean success;
        private List<ChatHistoryMessage> messages;
        private Integer totalCount;
    }
    
    /**
     * Chat History Message DTO
     */
    @Data
    public static class ChatHistoryMessage {
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private Long id;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private Long profileId;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private Long userId;
        
        private String role;
        private String content;
        private String messageType;
        
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private List<Long> attachmentIds;
        
        private Integer attachmentCount;
        private String interactionType;
        private Double confidence;
        private java.time.Instant createdAt;
        private java.time.Instant updatedAt;
    }
}
