package com.tala.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result from Event Extraction Service (Stage 3)
 * Extracts structured event data from user input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventExtractionResult {
    
    /**
     * Extracted events in structured format
     */
    private List<ExtractedEvent> events;
    
    /**
     * AI-generated response message
     */
    private String aiMessage;
    
    /**
     * Intent understanding
     */
    private String intentUnderstanding;
    
    /**
     * Confidence score (0.0 - 1.0)
     */
    private Double confidence;
    
    /**
     * Intent type: EVENT_RECORDING, CONVERSATION, QUESTION, MEDICAL_CONCERN
     */
    private String intent;
    
    /**
     * Data source type: HOME_EVENT, DAY_CARE_REPORT, INCIDENT_REPORT, HEALTH_REPORT, AI_CHAT
     */
    private String dataSourceType;
    
    /**
     * AI thinking process
     */
    private String aiThinkProcess;
    
    /**
     * Raw AI response for auditing
     */
    private String rawAiResponse;
    
    /**
     * Clarification questions (if data is incomplete)
     */
    private List<String> clarificationNeeded;
    
    /**
     * Individual extracted event
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedEvent {
        /**
         * Event category (JOURNAL, HEALTH)
         */
        private String eventCategory;
        
        /**
         * Event type (FEEDING, SLEEP, DIAPER, etc.)
         */
        private String eventType;
        
        /**
         * Event timestamp
         */
        private LocalDateTime timestamp;
        
        /**
         * Event summary
         */
        private String summary;
        
        /**
         * Structured event data (flexible key-value pairs)
         */
        private Map<String, Object> eventData;
        
        /**
         * Confidence for this specific event
         */
        private Double confidence;
    }
}
