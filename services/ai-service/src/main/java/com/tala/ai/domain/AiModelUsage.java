package com.tala.ai.domain;

import com.tala.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * AI Model Usage Record Entity
 * 
 * Purpose:
 * - Track every AI model invocation details
 * - Record token usage and costs
 * - Monitor performance metrics and error rates
 * - Support cost analysis and optimization
 * 
 * @author Tala Team
 */
@Entity
@Table(name = "ai_model_usage", schema = "ai", indexes = {
        @Index(name = "idx_profile_id", columnList = "profile_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_operation_type", columnList = "operation_type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelUsage extends BaseEntity {
    
    /**
     * Profile ID (if applicable)
     */
    @Column(name = "profile_id")
    private Long profileId;
    
    /**
     * User ID
     */
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    private Long userId = 1L;  // Default system user
    
    /**
     * AI model name (e.g., gemini-2.0-flash)
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;
    
    /**
     * Operation type
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;
    
    /**
     * Input token count
     */
    @Column(name = "input_tokens")
    private Integer inputTokens;
    
    /**
     * Output token count
     */
    @Column(name = "output_tokens")
    private Integer outputTokens;
    
    /**
     * Total token count
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    /**
     * Cache used flag (true if prompt caching was used)
     */
    @Column(name = "cache_used", nullable = false)
    @Builder.Default
    private Boolean cacheUsed = false;
    
    /**
     * Cached token count (tokens saved by using cache)
     */
    @Column(name = "cached_tokens")
    private Integer cachedTokens;
    
    /**
     * Dynamic token count (tokens actually sent, not from cache)
     */
    @Column(name = "dynamic_tokens")
    private Integer dynamicTokens;
    
    /**
     * User message length
     */
    @Column(name = "user_message_length")
    private Integer userMessageLength;
    
    /**
     * Static prompt length (cacheable part)
     */
    @Column(name = "static_prompt_length")
    private Integer staticPromptLength;
    
    /**
     * Dynamic context length (varies per request)
     */
    @Column(name = "dynamic_context_length")
    private Integer dynamicContextLength;
    
    /**
     * Estimated cost (USD)
     */
    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;
    
    /**
     * Input message content (system prompt + user message)
     */
    @Column(name = "input_message", columnDefinition = "MEDIUMTEXT")
    private String inputMessage;
    
    /**
     * AI response length
     */
    @Column(name = "response_length")
    private Integer responseLength;
    
    /**
     * Output message content (AI response)
     */
    @Column(name = "output_message", columnDefinition = "MEDIUMTEXT")
    private String outputMessage;
    
    /**
     * Raw JSON response (for debugging)
     */
    @Column(name = "raw_json_response", columnDefinition = "MEDIUMTEXT")
    private String rawJsonResponse;
    
    /**
     * Success flag
     */
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;
    
    /**
     * Error message
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Response latency (milliseconds)
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;
    
    /**
     * Has attachments flag
     */
    @Column(name = "has_attachments", nullable = false)
    @Builder.Default
    private Boolean hasAttachments = false;
    
    /**
     * Attachment count
     */
    @Column(name = "attachment_count")
    private Integer attachmentCount;
    
    /**
     * Pipeline stage name (for debugging and tracking)
     * e.g., AttachmentParser, ChatClassifier, EventExtraction
     */
    @Column(name = "stage_name", length = 100)
    private String stageName;
    
    /**
     * Calculate token savings percentage
     */
    public Double getTokenSavingsPercentage() {
        if (cachedTokens == null || cachedTokens == 0 || inputTokens == null || inputTokens == 0) {
            return 0.0;
        }
        return (cachedTokens.doubleValue() / (cachedTokens + dynamicTokens)) * 100.0;
    }
    
    /**
     * AI operation type enum
     */
    public enum OperationType {
        /** Structured response (JSON format) */
        STRUCTURED,
        
        /** Stream response (real-time push) */
        STREAM,
        
        /** Simple text response */
        SIMPLE,
        
        /** Attachment parsing */
        ATTACHMENT_PARSING,
        
        /** Chat classification */
        CHAT_CLASSIFICATION,
        
        /** Event extraction */
        EVENT_EXTRACTION
    }
}
