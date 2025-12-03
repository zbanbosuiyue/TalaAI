package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Mem0 Integration Service
 * 
 * Provides conversational memory capabilities:
 * - Store chat messages
 * - Retrieve chat history
 * - Search relevant memories
 * - Manage user/profile context
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Mem0Service {
    
    private final ObjectMapper objectMapper;
    
    @Value("${external.mem0.url:http://localhost:9002}")
    private String mem0Url;
    
    @Value("${external.mem0.enabled:true}")
    private boolean mem0Enabled;
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    
    /**
     * Add a chat message to mem0
     * 
     * @param userId User ID
     * @param profileId Baby profile ID
     * @param role Message role (user/assistant)
     * @param content Message content
     */
    public void addMessage(Long userId, Long profileId, String role, String content) {
        if (!mem0Enabled) {
            log.debug("Mem0 is disabled, skipping message storage");
            return;
        }
        
        try {
            // Use /add endpoint (not /v1/memories/)
            String url = mem0Url + "/add";
            
            // User key format: profile-{profileId}
            String mem0UserId = String.format("profile-%d", profileId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", mem0UserId);
            requestBody.put("messages", List.of(Map.of(
                    "role", role,
                    "content", content
            )));
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("profile_id", profileId);
            if (userId != null) {
                metadata.put("user_id", userId);
            }
            requestBody.put("metadata", metadata);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Successfully stored message in mem0 for user {} profile {}", userId, profileId);
                } else {
                    log.warn("Failed to store message in mem0: {}", response.code());
                }
            }
            
        } catch (Exception e) {
            log.error("Error storing message in mem0", e);
        }
    }
    
    /**
     * Get chat history from mem0
     * 
     * @param userId User ID
     * @param profileId Baby profile ID
     * @return Chat history as formatted string
     */
    public String getChatHistory(Long userId, Long profileId) {
        if (!mem0Enabled) {
            return null;
        }
        
        try {
            // User key format: profile-{profileId}
            String mem0UserId = String.format("profile-%d", profileId);
            String url = mem0Url + "/get_all?user_id=" + mem0UserId + "&limit=10";
            
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to retrieve chat history from mem0: {}", response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode memories = root.path("results");
                
                if (!memories.isArray() || memories.size() == 0) {
                    return null;
                }
                
                // Format chat history
                StringBuilder history = new StringBuilder();
                for (JsonNode memory : memories) {
                    String memoryText = memory.path("memory").asText();
                    history.append(memoryText).append("\n");
                }
                
                return history.toString();
            }
            
        } catch (Exception e) {
            log.error("Error retrieving chat history from mem0", e);
            return null;
        }
    }
    
    /**
     * Search for relevant memories based on query
     * 
     * @param query Search query
     * @param userId User ID
     * @param profileId Baby profile ID
     * @return Relevant memories as formatted string
     */
    public String getRelevantMemories(String query, Long userId, Long profileId) {
        if (!mem0Enabled) {
            return null;
        }
        
        try {
            // User key format: profile-{profileId}
            String mem0UserId = String.format("profile-%d", profileId);
            // Use /search endpoint (not /v1/memories/search/)
            String url = mem0Url + "/search";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", mem0UserId);
            requestBody.put("query", query);
            requestBody.put("limit", 5);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to search memories in mem0: {}", response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                // Mem0 response format: {"status": "ok", "result": {"results": [...]}}
                JsonNode resultNode = root.path("result");
                if (resultNode.isMissingNode()) {
                    return null;
                }
                JsonNode results = resultNode.path("results");
                
                if (!results.isArray() || results.size() == 0) {
                    return null;
                }
                
                // Format relevant memories
                StringBuilder memories = new StringBuilder();
                for (JsonNode result : results) {
                    String memoryText = result.path("memory").asText();
                    double score = result.path("score").asDouble(0.0);
                    
                    if (score > 0.5) {  // Only include relevant memories
                        memories.append("- ").append(memoryText).append("\n");
                    }
                }
                
                return memories.length() > 0 ? memories.toString() : null;
            }
            
        } catch (Exception e) {
            log.error("Error searching memories in mem0", e);
            return null;
        }
    }
    
    /**
     * Delete all memories for a user/profile
     * 
     * @param userId User ID
     * @param profileId Baby profile ID
     */
    public void clearMemories(Long userId, Long profileId) {
        if (!mem0Enabled) {
            return;
        }
        
        try {
            String mem0UserId = String.format("user_%d_profile_%d", userId, profileId);
            String url = mem0Url + "/v1/memories/?user_id=" + mem0UserId;
            
            Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Successfully cleared memories for user {} profile {}", userId, profileId);
                } else {
                    log.warn("Failed to clear memories: {}", response.code());
                }
            }
            
        } catch (Exception e) {
            log.error("Error clearing memories in mem0", e);
        }
    }
    
    /**
     * Check if mem0 is available
     */
    public boolean isAvailable() {
        if (!mem0Enabled) {
            return false;
        }
        
        try {
            String url = mem0Url + "/health";
            
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            log.debug("Mem0 is not available: {}", e.getMessage());
            return false;
        }
    }
}
