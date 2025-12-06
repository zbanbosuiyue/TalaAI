package com.tala.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result from Chat Classifier Service (Stage 2)
 * Determines user intent and interaction type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatClassificationResult {
    
    /**
     * Classified interaction type
     */
    private InteractionType interactionType;
    
    /**
     * Classification reason/explanation
     */
    private String classificationReason;
    
    /**
     * Confidence score (0.0 - 1.0)
     */
    private Double confidence;
    
    /**
     * AI thinking process
     */
    private String aiThinkProcess;
    
    /**
     * Raw AI response for auditing
     */
    private String rawAiResponse;
    
    /**
     * Original user input
     */
    private String userInput;
    
    /**
     * Interaction type classification
     */
    public enum InteractionType {
        /**
         * User wants to record/log baby data
         */
        DATA_RECORDING,
        
        /**
         * User is uploading curriculum documents
         */
        CURRICULUM_UPLOAD,
        
        /**
         * User is asking questions or seeking advice
         */
        QUESTION_ANSWERING,
        
        /**
         * General conversation/chat
         */
        GENERAL_CHAT,
        
        /**
         * Out of scope or unclear
         */
        OUT_OF_SCOPE
    }
}
