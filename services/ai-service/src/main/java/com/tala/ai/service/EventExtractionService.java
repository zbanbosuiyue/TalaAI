package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.dto.EventExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 3: Event Extraction Service
 * 
 * Extracts structured event data from user input based on:
 * - User message (text or voice transcription)
 * - Attachment content (if parsed)
 * - Chat history context
 * - Baby profile data
 * 
 * Outputs structured JSON/TOON format for origin-data-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventExtractionService {
    
    private final GeminiService geminiService;
    private final SystemPromptManager systemPromptManager;
    private final ObjectMapper objectMapper;
    
    /**
     * Extract structured event data from user input
     * 
     * @param userMessage User's message
     * @param attachmentContext Parsed attachment content (if any)
     * @param babyProfileContext Baby profile information
     * @param chatHistory Recent chat history
     * @param userLocalTime User's local time for timestamp calculation
     * @return Extracted events in structured format
     */
    public EventExtractionResult extractEvents(String userMessage,
                                                String attachmentContext,
                                                String babyProfileContext,
                                                String chatHistory,
                                                String userLocalTime) {
        return extractEvents(userMessage, attachmentContext, babyProfileContext, chatHistory, userLocalTime, null, null);
    }
    
    /**
     * Extract structured event data with tracking
     */
    public EventExtractionResult extractEvents(String userMessage,
                                                String attachmentContext,
                                                String babyProfileContext,
                                                String chatHistory,
                                                String userLocalTime,
                                                Long profileId,
                                                Long userId) {
        log.info("Extracting events from user input");
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return EventExtractionResult.builder()
                    .events(new ArrayList<>())
                    .aiMessage("I didn't receive any message. Could you please tell me what you'd like to record?")
                    .intentUnderstanding("Empty input")
                    .confidence(0.0)
                    .build();
        }
        
        try {
            // Get cached system prompt (returns null if caching unavailable)
            String cachedPromptId = systemPromptManager.getEventExtractionCache();
            
            // Build dynamic context (not cached)
            String dynamicContext = buildDynamicContext(attachmentContext, babyProfileContext, 
                    chatHistory, userLocalTime);
            
            String aiResponse;
            if (cachedPromptId != null) {
                // Use cached prompt (preferred)
                log.debug("Using cached system prompt for event extraction");
                aiResponse = geminiService.generateContentWithCache(
                        cachedPromptId, dynamicContext, userMessage, profileId, userId, "EventExtraction");
            } else {
                // Fallback to non-cached mode
                log.debug("Cache unavailable, using non-cached mode for event extraction");
                String systemPrompt = systemPromptManager.getEventExtractionSystemPrompt();
                aiResponse = geminiService.generateContentWithSystemPrompt(
                        systemPrompt, dynamicContext, userMessage, profileId, userId, "EventExtraction");
            }
            
            // Parse response
            EventExtractionResult result = parseExtractionResponse(aiResponse);
            result.setRawAiResponse(aiResponse);
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to extract events", e);
            return EventExtractionResult.builder()
                    .events(new ArrayList<>())
                    .aiMessage("I'm having trouble understanding. Could you rephrase that?")
                    .intentUnderstanding("Extraction failed")
                    .confidence(0.0)
                    .aiThinkProcess("Error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Build dynamic context (not cached)
     * This includes chat history, baby profile, attachments, and current time
     */
    private String buildDynamicContext(String attachmentContext,
                                       String babyProfileContext,
                                       String chatHistory,
                                       String userLocalTime) {
        StringBuilder context = new StringBuilder();
        
        // Add current time context
        if (userLocalTime != null && !userLocalTime.isBlank()) {
            context.append("⏰ CURRENT SYSTEM TIME: ").append(userLocalTime).append("\n");
            context.append("⚠️ ALL timestamp calculations MUST be based on this current time!\n\n");
        }
        
        // Add baby profile context
        if (babyProfileContext != null && !babyProfileContext.isBlank()) {
            context.append("=== BABY PROFILE ===\n");
            context.append(babyProfileContext).append("\n\n");
        }
        
        // Add attachment context
        if (attachmentContext != null && !attachmentContext.isBlank()) {
            context.append("=== ATTACHMENT CONTENT ===\n");
            context.append(attachmentContext).append("\n\n");
            context.append("⚠️ Extract events from attachment content. Use dates from attachment if specified.\n\n");
        }
        
        // Add chat history
        if (chatHistory != null && !chatHistory.isBlank()) {
            context.append("=== RECENT CHAT HISTORY ===\n");
            context.append(chatHistory).append("\n\n");
            context.append("⚠️ Use chat history to understand context and continuation.\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * Parse AI extraction response
     */
    private EventExtractionResult parseExtractionResponse(String aiResponse) throws Exception {
        String json = extractJson(aiResponse);
        JsonNode root = objectMapper.readTree(json);
        
        String aiMessage = root.path("ai_message").asText("Got it! I've processed your request.");
        String intentUnderstanding = root.path("intent_understanding").asText("");
        double confidence = root.path("confidence").asDouble(0.8);
        String intent = root.path("intent").asText("");
        String dataSourceType = root.path("data_source_type").asText("");
        String aiThinkProcess = root.path("ai_think_process").asText("");
        
        // Parse events
        List<EventExtractionResult.ExtractedEvent> events = new ArrayList<>();
        JsonNode eventsNode = root.path("events");
        if (eventsNode.isArray()) {
            for (JsonNode eventNode : eventsNode) {
                EventExtractionResult.ExtractedEvent event = parseEvent(eventNode);
                events.add(event);
            }
        }
        
        // Parse clarification questions
        List<String> clarifications = new ArrayList<>();
        JsonNode clarNode = root.path("clarification_needed");
        if (clarNode.isArray()) {
            for (JsonNode clar : clarNode) {
                clarifications.add(clar.asText());
            }
        }
        
        return EventExtractionResult.builder()
                .aiMessage(aiMessage)
                .intentUnderstanding(intentUnderstanding)
                .confidence(confidence)
                .intent(intent)
                .dataSourceType(dataSourceType)
                .events(events)
                .clarificationNeeded(clarifications)
                .aiThinkProcess(aiThinkProcess)
                .build();
    }
    
    /**
     * Parse individual event
     */
    private EventExtractionResult.ExtractedEvent parseEvent(JsonNode eventNode) {
        String eventCategory = eventNode.path("event_category").asText();
        String eventType = eventNode.path("event_type").asText();
        String timestampStr = eventNode.path("timestamp").asText();
        String summary = eventNode.path("summary").asText();
        double confidence = eventNode.path("confidence").asDouble(0.8);
        
        // Parse timestamp
        LocalDateTime timestamp = null;
        if (!timestampStr.isBlank()) {
            try {
                timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                log.warn("Failed to parse timestamp: {}", timestampStr);
            }
        }
        
        // Parse event data (flexible key-value pairs)
        Map<String, Object> eventData = new HashMap<>();
        JsonNode dataNode = eventNode.path("event_data");
        if (dataNode.isObject()) {
            dataNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isNumber()) {
                    eventData.put(key, value.asDouble());
                } else if (value.isBoolean()) {
                    eventData.put(key, value.asBoolean());
                } else if (value.isArray()) {
                    // Handle arrays (e.g., ai_tags)
                    List<String> arrayValues = new ArrayList<>();
                    value.forEach(item -> arrayValues.add(item.asText()));
                    eventData.put(key, arrayValues);
                } else {
                    eventData.put(key, value.asText());
                }
            });
        }
        
        return EventExtractionResult.ExtractedEvent.builder()
                .eventCategory(eventCategory)
                .eventType(eventType)
                .timestamp(timestamp)
                .summary(summary)
                .eventData(eventData)
                .confidence(confidence)
                .build();
    }
    
    /**
     * Extract JSON from AI response (remove markdown formatting)
     */
    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        
        // Try to extract from ```json ... ``` blocks
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.lastIndexOf("```");
            if (start > 6 && end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        // Try to extract from ``` ... ``` blocks
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            if (start > 2 && end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        // Fallback: extract JSON between first { and last }
        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1).trim();
        }
        
        return response.trim();
    }
}
