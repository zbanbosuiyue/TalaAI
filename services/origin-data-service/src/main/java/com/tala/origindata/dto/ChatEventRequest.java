package com.tala.origindata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for chat-based event creation
 * Receives AI-processed data from ai-service
 * 
 * Supports multimodal chat with attachments (images, documents, audio)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventRequest {
    
    private Long profileId;
    private String userMessage;
    private String aiMessage;
    private String intent;  // EVENT_RECORDING, CONVERSATION, QUESTION, MEDICAL_CONCERN
    private String dataSourceType;  // HOME_EVENT, DAY_CARE_REPORT, INCIDENT_REPORT, HEALTH_REPORT, AI_CHAT
    private Double confidence;
    private String intentUnderstanding;
    private List<ExtractedEvent> events;
    
    /**
     * Attachment file IDs from file-service
     * These IDs will be stored in OriginalEvent.attachmentIds
     */
    private List<Long> attachmentFileIds;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedEvent {
        private String eventCategory;  // JOURNAL or HEALTH
        private String eventType;      // FEEDING, SLEEP, DIAPER, etc.
        private LocalDateTime timestamp;
        private String summary;
        private Map<String, Object> eventData;
        private Double confidence;
    }
}
