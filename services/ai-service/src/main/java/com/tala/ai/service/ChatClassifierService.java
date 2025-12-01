package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.dto.ChatClassificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stage 2: Chat Classifier Service
 * 
 * Determines user intent and interaction type:
 * - DATA_RECORDING: User wants to log baby data
 * - QUESTION_ANSWERING: User is asking questions
 * - GENERAL_CHAT: General conversation
 * - OUT_OF_SCOPE: Unrelated topics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatClassifierService {
    
    private final GeminiService geminiService;
    private final SystemPromptManager systemPromptManager;
    private final ObjectMapper objectMapper;
    
    /**
     * Classify user chat to determine interaction type
     * 
     * @param userInput User's message
     * @param attachmentContext Context from parsed attachments (if any)
     * @param chatHistory Recent chat history for context
     * @return Classification result
     */
    public ChatClassificationResult classifyChat(String userInput, 
                                                  String attachmentContext,
                                                  String chatHistory) {
        return classifyChat(userInput, attachmentContext, chatHistory, null, null);
    }
    
    /**
     * Classify user chat with tracking
     */
    public ChatClassificationResult classifyChat(String userInput, 
                                                  String attachmentContext,
                                                  String chatHistory,
                                                  Long profileId,
                                                  Long userId) {
        log.info("Classifying user input: {}", userInput != null ? userInput.substring(0, Math.min(50, userInput.length())) : "null");
        
        if (userInput == null || userInput.trim().isEmpty()) {
            return ChatClassificationResult.builder()
                    .interactionType(ChatClassificationResult.InteractionType.OUT_OF_SCOPE)
                    .classificationReason("Empty input")
                    .confidence(1.0)
                    .userInput(userInput)
                    .build();
        }
        
        try {
            // Get cached system prompt
            String cachedPromptId = systemPromptManager.getChatClassifierCache();
            
            // Build dynamic context
            String dynamicContext = buildDynamicContext(attachmentContext, chatHistory);
            
            // Call Gemini with cached prompt and tracking
            String aiResponse = geminiService.generateContentWithCache(
                    cachedPromptId, dynamicContext, userInput, profileId, userId, "ChatClassification");
            
            // Parse response
            ChatClassificationResult result = parseClassificationResponse(aiResponse, userInput);
            result.setRawAiResponse(aiResponse);
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to classify chat", e);
            // Default to QUESTION_ANSWERING as safe fallback
            return ChatClassificationResult.builder()
                    .interactionType(ChatClassificationResult.InteractionType.QUESTION_ANSWERING)
                    .classificationReason("Classification failed, defaulting to Q&A: " + e.getMessage())
                    .confidence(0.3)
                    .userInput(userInput)
                    .aiThinkProcess("Error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Build dynamic context (not cached)
     */
    private String buildDynamicContext(String attachmentContext, String chatHistory) {
        StringBuilder context = new StringBuilder();
        
        // Add attachment context if available
        if (attachmentContext != null && !attachmentContext.isBlank()) {
            context.append("\n=== ATTACHMENT CONTEXT ===\n");
            context.append(attachmentContext).append("\n\n");
            context.append("⚠️ User has attachments. If they contain structured data (daycare report, medical record), ");
            context.append("this is likely DATA_RECORDING intent.\n\n");
        }
        
        // Add chat history if available
        if (chatHistory != null && !chatHistory.isBlank()) {
            context.append("\n=== RECENT CHAT HISTORY ===\n");
            context.append(chatHistory).append("\n\n");
            context.append("⚠️ Use chat history to understand context and continuation.\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * Parse AI classification response
     */
    private ChatClassificationResult parseClassificationResponse(String aiResponse, String userInput) throws Exception {
        String json = extractJson(aiResponse);
        JsonNode root = objectMapper.readTree(json);
        
        String interactionTypeStr = root.path("interaction_type").asText();
        String reason = root.path("reason").asText("AI classification");
        double confidence = root.path("confidence").asDouble(0.8);
        String aiThinkProcess = root.path("ai_think_process").asText("");
        
        ChatClassificationResult.InteractionType interactionType = 
                ChatClassificationResult.InteractionType.valueOf(interactionTypeStr);
        
        return ChatClassificationResult.builder()
                .interactionType(interactionType)
                .classificationReason(reason)
                .confidence(confidence)
                .aiThinkProcess(aiThinkProcess)
                .userInput(userInput)
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
