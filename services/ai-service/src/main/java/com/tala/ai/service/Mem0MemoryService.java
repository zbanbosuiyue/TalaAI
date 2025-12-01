package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.client.Mem0Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mem0 Memory Service
 * 
 * Provides memory storage and retrieval capabilities using Mem0
 * 
 * @author Tala Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Mem0MemoryService {
    
    private final Mem0Client mem0Client;
    private final ObjectMapper objectMapper;
    
    @Value("${external.mem0.enabled:true}")
    private boolean mem0Enabled;
    
    /**
     * Add memory for a user
     * 
     * @param userId User ID
     * @param userMessage User message content
     * @param aiMessage AI response content
     * @param metadata Additional metadata
     */
    public void addMemory(String userId, String userMessage, String aiMessage, Map<String, Object> metadata) {
        if (!mem0Enabled) {
            log.debug("Mem0 is disabled, skipping memory addition");
            return;
        }
        
        try {
            String metadataJson = metadata != null && !metadata.isEmpty() 
                    ? objectMapper.writeValueAsString(metadata) 
                    : "{}";
            
            // Add user message and AI response as conversation context
            String content = String.format("User: %s\nAssistant: %s", userMessage, aiMessage);
            
            String response = mem0Client.addMemory(userId, content, metadataJson);
            log.debug("✅ Added memory to Mem0 for user {}: {}", userId, response);
        } catch (Exception e) {
            // Memory addition failure should not affect main flow
            log.warn("⚠️ Failed to add memory to Mem0 for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Search memories for a user
     * 
     * @param userId User ID
     * @param query Search query
     * @param limit Maximum number of results
     * @return List of relevant memories
     */
    public List<String> searchMemories(String userId, String query, int limit) {
        if (!mem0Enabled) {
            log.debug("Mem0 is disabled, returning empty memories");
            return new ArrayList<>();
        }
        
        try {
            String response = mem0Client.searchMemories(userId, query);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            List<String> memories = new ArrayList<>();
            if (jsonNode.has("results") && jsonNode.get("results").isArray()) {
                for (JsonNode result : jsonNode.get("results")) {
                    if (result.has("memory")) {
                        memories.add(result.get("memory").asText());
                        if (memories.size() >= limit) {
                            break;
                        }
                    }
                }
            }
            
            log.debug("✅ Found {} memories for user {} with query: {}", memories.size(), userId, query);
            return memories;
        } catch (Exception e) {
            log.warn("⚠️ Failed to search memories from Mem0 for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all memories for a user
     * 
     * @param userId User ID
     * @return List of all memories
     */
    public List<String> getUserMemories(String userId) {
        if (!mem0Enabled) {
            log.debug("Mem0 is disabled, returning empty memories");
            return new ArrayList<>();
        }
        
        try {
            String response = mem0Client.getUserMemories(userId);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            List<String> memories = new ArrayList<>();
            if (jsonNode.has("results") && jsonNode.get("results").isArray()) {
                for (JsonNode result : jsonNode.get("results")) {
                    if (result.has("memory")) {
                        memories.add(result.get("memory").asText());
                    }
                }
            }
            
            log.debug("✅ Retrieved {} memories for user {}", memories.size(), userId);
            return memories;
        } catch (Exception e) {
            log.warn("⚠️ Failed to get memories from Mem0 for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Build memory context for AI prompts
     * 
     * @param userId User ID
     * @param query Current query/context
     * @param maxMemories Maximum number of memories to include
     * @return Formatted memory context string
     */
    public String buildMemoryContext(String userId, String query, int maxMemories) {
        List<String> memories = searchMemories(userId, query, maxMemories);
        
        if (memories.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("\n\n📝 RELEVANT MEMORIES:\n");
        for (int i = 0; i < memories.size(); i++) {
            context.append(String.format("%d. %s\n", i + 1, memories.get(i)));
        }
        
        return context.toString();
    }
}
