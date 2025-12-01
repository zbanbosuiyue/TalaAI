package com.tala.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.domain.ChatMessage;
import com.tala.ai.dto.EventExtractionResult;
import com.tala.ai.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat Message Service
 * 
 * Handles storing and retrieving chat messages
 * 
 * @author Tala Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Store user message without attachments
     */
    @Transactional
    public ChatMessage storeUserMessage(Long profileId, Long userId, String content) {
        return storeUserMessage(profileId, userId, content, null);
    }
    
    /**
     * Store user message with attachments
     */
    @Transactional
    public ChatMessage storeUserMessage(Long profileId, Long userId, String content, List<Long> attachmentIds) {
        try {
            ChatMessage message = ChatMessage.builder()
                    .profileId(profileId)
                    .userId(userId)
                    .role(ChatMessage.MessageRole.USER)
                    .content(content)
                    .messageType(ChatMessage.MessageType.TEXT)
                    .attachmentIds(attachmentIds != null ? attachmentIds : List.of())
                    .build();
            
            ChatMessage saved = chatMessageRepository.save(message);
            
            log.info("Stored user message: id={}, profileId={}, userId={}, attachments={}", 
                    saved.getId(), profileId, userId, saved.getAttachmentCount());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to store user message", e);
            throw new RuntimeException("Failed to store user message", e);
        }
    }
    
    /**
     * Store assistant message
     */
    @Transactional
    public ChatMessage storeAssistantMessage(Long profileId, Long userId, String content,
                                             String interactionType, Double confidence,
                                             EventExtractionResult extractionResult,
                                             String thinkingProcess) {
        try {
            ChatMessage.ChatMessageBuilder builder = ChatMessage.builder()
                    .profileId(profileId)
                    .userId(userId)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .content(content)
                    .messageType(ChatMessage.MessageType.TEXT)
                    .interactionType(interactionType)
                    .confidence(confidence);
            
            // Store extracted records as JSON
            if (extractionResult != null && extractionResult.getEvents() != null && !extractionResult.getEvents().isEmpty()) {
                String extractedRecordsJson = objectMapper.writeValueAsString(extractionResult.getEvents());
                builder.extractedRecordsJson(extractedRecordsJson);
                builder.messageType(ChatMessage.MessageType.EVENT);
            }
            
            // Store thinking process
            if (thinkingProcess != null && !thinkingProcess.isBlank()) {
                builder.thinkingProcess(thinkingProcess);
            }
            
            // Store metadata
            Map<String, Object> metadata = new HashMap<>();
            if (extractionResult != null) {
                metadata.put("intentUnderstanding", extractionResult.getIntentUnderstanding());
                metadata.put("confidence", extractionResult.getConfidence());
                metadata.put("clarificationNeeded", extractionResult.getClarificationNeeded());
            }
            String metadataJson = objectMapper.writeValueAsString(metadata);
            builder.metadata(metadataJson);
            
            ChatMessage message = builder.build();
            ChatMessage saved = chatMessageRepository.save(message);
            
            log.info("Stored assistant message: id={}, profileId={}, userId={}, type={}", 
                    saved.getId(), profileId, userId, saved.getMessageType());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to store assistant message", e);
            throw new RuntimeException("Failed to store assistant message", e);
        }
    }
    
    /**
     * Get recent messages for context (last N messages)
     */
    public List<ChatMessage> getRecentMessages(Long profileId, int limit) {
        return chatMessageRepository.findRecentMessagesByProfileId(profileId, PageRequest.of(0, limit));
    }
    
    /**
     * Get all messages for a profile
     */
    public List<ChatMessage> getAllMessages(Long profileId) {
        return chatMessageRepository.findAllByProfileIdAndNotDeleted(profileId);
    }
    
    /**
     * Format chat history for AI context
     * Includes attachment information for multimodal understanding
     */
    public String formatChatHistoryForAI(Long profileId, int limit) {
        List<ChatMessage> messages = getRecentMessages(profileId, limit);
        
        if (messages.isEmpty()) {
            return null;
        }
        
        StringBuilder history = new StringBuilder();
        
        // Reverse to get chronological order (oldest first)
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            String role = msg.getRole() == ChatMessage.MessageRole.USER ? "User" : "Assistant";
            history.append(role).append(": ").append(msg.getContent());
            
            // Include attachment context for multimodal understanding
            if (msg.hasAttachments()) {
                history.append(" [").append(msg.getAttachmentCount()).append(" attachment(s)]");
            }
            
            history.append("\n");
        }
        
        return history.toString();
    }
    
    /**
     * Count messages for a profile
     */
    public long countMessages(Long profileId) {
        return chatMessageRepository.countByProfileIdAndNotDeleted(profileId);
    }
}
