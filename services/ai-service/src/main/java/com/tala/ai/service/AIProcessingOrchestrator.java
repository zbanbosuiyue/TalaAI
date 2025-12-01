package com.tala.ai.service;

import com.tala.ai.dto.AttachmentParserResult;
import com.tala.ai.dto.ChatClassificationResult;
import com.tala.ai.dto.EventExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Processing Orchestrator
 * 
 * Coordinates the 3-stage AI processing pipeline:
 * 1. Attachment Parser (if attachments present)
 * 2. Chat Classifier (determine user intent)
 * 3. Event Extraction (extract structured data if recording intent)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProcessingOrchestrator {
    
    private final AttachmentParserService attachmentParserService;
    private final ChatClassifierService chatClassifierService;
    private final EventExtractionService eventExtractionService;
    
    /**
     * Process user input through the complete AI pipeline
     * 
     * @param request Processing request
     * @return Processing result with all stage outputs
     */
    public ProcessingResult processInput(ProcessingRequest request) {
        log.info("Starting AI processing pipeline for user input");
        
        ProcessingResult result = new ProcessingResult();
        
        try {
            // Stage 1: Parse attachments (if present)
            AttachmentParserResult attachmentResult = null;
            if (request.attachmentUrls != null && !request.attachmentUrls.isEmpty()) {
                log.info("Stage 1: Parsing {} attachments", request.attachmentUrls.size());
                attachmentResult = attachmentParserService.parseAttachments(
                        request.attachmentUrls, 
                        request.userMessage,
                        request.profileId,
                        request.userId);
                result.attachmentParserResult = attachmentResult;
            }
            
            // Build attachment context for next stages
            String attachmentContext = buildAttachmentContext(attachmentResult);
            
            // Stage 2: Classify chat interaction type
            log.info("Stage 2: Classifying chat interaction");
            ChatClassificationResult classificationResult = chatClassifierService.classifyChat(
                    request.userMessage,
                    attachmentContext,
                    request.chatHistory,
                    request.profileId,
                    request.userId);
            result.chatClassificationResult = classificationResult;
            
            // Stage 3: Extract events (only if DATA_RECORDING intent)
            if (classificationResult.getInteractionType() == ChatClassificationResult.InteractionType.DATA_RECORDING) {
                log.info("Stage 3: Extracting structured event data");
                EventExtractionResult extractionResult = eventExtractionService.extractEvents(
                        request.userMessage,
                        attachmentContext,
                        request.babyProfileContext,
                        request.chatHistory,
                        request.userLocalTime,
                        request.profileId,
                        request.userId);
                result.eventExtractionResult = extractionResult;
            } else {
                log.info("Stage 3: Skipped (interaction type: {})", classificationResult.getInteractionType());
                // For non-recording intents, create empty result with appropriate message
                result.eventExtractionResult = EventExtractionResult.builder()
                        .aiMessage(generateResponseForNonRecording(classificationResult))
                        .intentUnderstanding(classificationResult.getClassificationReason())
                        .confidence(classificationResult.getConfidence())
                        .events(List.of())
                        .build();
            }
            
            result.success = true;
            log.info("AI processing pipeline completed successfully");
            
        } catch (Exception e) {
            log.error("AI processing pipeline failed", e);
            result.success = false;
            result.errorMessage = "Processing failed: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Build attachment context summary for downstream stages
     */
    private String buildAttachmentContext(AttachmentParserResult attachmentResult) {
        if (attachmentResult == null || attachmentResult.getAttachments() == null || attachmentResult.getAttachments().isEmpty()) {
            return null;
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Attachment Summary:\n");
        context.append("Type: ").append(attachmentResult.getAttachmentType()).append("\n");
        context.append("Overall: ").append(attachmentResult.getOverallSummary()).append("\n\n");
        
        for (AttachmentParserResult.AttachmentSummary attachment : attachmentResult.getAttachments()) {
            context.append("File: ").append(attachment.getFileName()).append("\n");
            context.append("Summary: ").append(attachment.getContentSummary()).append("\n");
            
            if (attachment.getExtractedText() != null && !attachment.getExtractedText().isBlank()) {
                // Truncate long text
                String text = attachment.getExtractedText();
                if (text.length() > 500) {
                    text = text.substring(0, 500) + "...";
                }
                context.append("Content: ").append(text).append("\n");
            }
            
            if (attachment.getKeyFindings() != null && !attachment.getKeyFindings().isEmpty()) {
                context.append("Key Findings:\n");
                for (String finding : attachment.getKeyFindings()) {
                    context.append("  - ").append(finding).append("\n");
                }
            }
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Generate appropriate response for non-recording intents
     */
    private String generateResponseForNonRecording(ChatClassificationResult classification) {
        return switch (classification.getInteractionType()) {
            case QUESTION_ANSWERING -> 
                "I'd be happy to help answer your question! However, I need more context about your baby's data to provide a helpful response.";
            case GENERAL_CHAT -> 
                "Thank you for chatting with me! How can I help you with your baby's care today?";
            case OUT_OF_SCOPE -> 
                "I'm here to help with baby tracking and parenting support. Could you ask me something related to your baby's care?";
            default -> 
                "I'm not sure how to help with that. Could you rephrase your request?";
        };
    }
    
    /**
     * Processing request input
     */
    public static class ProcessingRequest {
        public String userMessage;
        public List<String> attachmentUrls;
        public String babyProfileContext;
        public String chatHistory;
        public String userLocalTime;
        public Long profileId;
        public Long userId;
    }
    
    /**
     * Processing result output
     */
    public static class ProcessingResult {
        public boolean success;
        public String errorMessage;
        public AttachmentParserResult attachmentParserResult;
        public ChatClassificationResult chatClassificationResult;
        public EventExtractionResult eventExtractionResult;
    }
}
