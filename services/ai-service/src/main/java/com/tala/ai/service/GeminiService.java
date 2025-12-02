package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.domain.AiModelUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Gemini 2.5 Flash integration service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {
    
    private final ObjectMapper objectMapper;
    private final AiModelUsageService aiModelUsageService;
    
    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Value("${gemini.model:gemini-2.5-pro-latest}")
    private String model;
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String GEMINI_STREAM_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?alt=sse&key=%s";
    private static final String GEMINI_CACHE_API_URL = "https://generativelanguage.googleapis.com/v1beta/cachedContents?key=%s";
    private static final String GEMINI_CACHE_GET_URL = "https://generativelanguage.googleapis.com/v1beta/cachedContents/%s?key=%s";
    
    // Cache TTL: 1 hour (3600 seconds) - Gemini minimum is 60s, maximum is 24h
    private static final int CACHE_TTL_SECONDS = 3600;
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();
    
    /**
     * Generate content with Gemini (non-streaming)
     */
    public String generateContent(String prompt) throws IOException {
        return generateContent(prompt, null, null, null);
    }
    
    /**
     * Generate content with Gemini (non-streaming) with usage tracking
     */
    public String generateContent(String prompt, Long profileId, Long userId, String stageName) throws IOException {
        String url = String.format(GEMINI_API_URL, model, apiKey);
        
        String requestBody = buildRequestBody(prompt);
        long startTime = System.currentTimeMillis();
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            
            if (!response.isSuccessful()) {
                // Record failed usage
                recordUsage(profileId, userId, stageName, prompt, null, null, null, null, null, 
                        false, "Gemini API error: " + response.code(), latencyMs, false, null, null);
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            
            // Extract token counts from response
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usageMetadata = root.path("usageMetadata");
            Integer inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
            Integer outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
            Integer totalTokens = usageMetadata.path("totalTokenCount").asInt(0);
            
            String extractedText = extractTextFromResponse(responseBody);
            
            // Record successful usage with token counts
            recordUsage(profileId, userId, stageName, prompt, extractedText, responseBody, 
                    inputTokens, outputTokens, totalTokens, true, null, latencyMs, false, null, null);
            
            return extractedText;
        } catch (IOException e) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            recordUsage(profileId, userId, stageName, prompt, null, null, null, null, null, 
                    false, e.getMessage(), latencyMs, false, null, null);
            throw e;
        }
    }
    
    /**
     * Generate content with Gemini (streaming via SSE)
     */
    public void generateContentStream(String prompt, SseEmitter emitter) {
        String url = String.format(GEMINI_STREAM_URL, model, apiKey);
        
        String requestBody = buildRequestBody(prompt);
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Gemini streaming failed", e);
                emitter.completeWithError(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    emitter.completeWithError(new IOException("Gemini API error: " + response.code()));
                    return;
                }
                
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                emitter.complete();
                                break;
                            }
                            
                            try {
                                String text = extractTextFromStreamChunk(data);
                                if (text != null && !text.isEmpty()) {
                                    emitter.send(SseEmitter.event()
                                        .data(text)
                                        .name("message"));
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse stream chunk: {}", e.getMessage());
                            }
                        }
                    }
                    
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Error processing stream", e);
                    emitter.completeWithError(e);
                }
            }
        });
    }
    
    /**
     * Generate content with system instruction and context
     */
    public String generateWithContext(String systemInstruction, String userPrompt, String context) throws IOException {
        String combinedPrompt = String.format(
            "System: %s\n\nContext: %s\n\nUser: %s",
            systemInstruction, context, userPrompt
        );
        return generateContent(combinedPrompt);
    }
    
    /**
     * Create or get cached content for system prompts
     * Uses Gemini Context Caching API to cache static system instructions
     * 
     * @param systemPrompt Static system prompt to cache
     * @param cacheName Unique name for this cache (e.g., "event-extraction-v1")
     * @return Cache name that can be used in subsequent requests
     */
    public String createCachedContent(String systemPrompt, String cacheName) throws IOException {
        String url = String.format(GEMINI_CACHE_API_URL, apiKey);
        
        String requestBody = String.format("""
            {
              "model": "models/%s",
              "displayName": "%s",
              "systemInstruction": {
                "parts": [{
                  "text": "%s"
                }]
              },
              "ttl": "%ds"
            }
            """, model, cacheName, escapeJson(systemPrompt), CACHE_TTL_SECONDS);
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                log.error("Failed to create cached content: {} - Response: {}", response.code(), responseBody);
                throw new IOException("Failed to create cached content: " + response.code() + " - " + responseBody);
            }
            
            JsonNode root = objectMapper.readTree(responseBody);
            String cacheId = root.path("name").asText();
            
            if (cacheId == null || cacheId.isEmpty()) {
                log.error("Cache creation returned empty cache ID. Response: {}", responseBody);
                throw new IOException("Cache creation failed: empty cache ID");
            }
            
            log.info("Created cached content: {}", cacheId);
            return cacheId;
        }
    }
    
    /**
     * Generate content with cached system prompt (Industry Best Practice)
     * 
     * Uses Gemini Context Caching API to avoid re-sending static system prompts.
     * This significantly reduces token usage and latency for repeated calls.
     * 
     * @param cachedContentName Name of cached content (from createCachedContent)
     * @param dynamicContext Dynamic context (chat history, baby profile, etc.)
     * @param userMessage User message
     * @param profileId Profile ID for tracking
     * @param userId User ID for tracking
     * @param stageName Stage name for tracking
     * @return AI generated text
     */
    public String generateContentWithCache(String cachedContentName, String dynamicContext, 
                                          String userMessage, Long profileId, Long userId, 
                                          String stageName) throws IOException {
        String url = String.format(GEMINI_API_URL, model, apiKey);
        
        String requestBody = buildRequestBodyWithCachedContent(cachedContentName, dynamicContext, userMessage);
        long startTime = System.currentTimeMillis();
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            
            if (!response.isSuccessful()) {
                String combinedPrompt = "[CACHED]" + "\n" + (dynamicContext != null ? dynamicContext : "") + "\n" + userMessage;
                recordUsage(profileId, userId, stageName, combinedPrompt, null, null, null, null, null, 
                        false, "Gemini API error: " + response.code(), latencyMs, true, null, null);
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            
            // Extract token counts from response
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usageMetadata = root.path("usageMetadata");
            Integer inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
            Integer outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
            Integer totalTokens = usageMetadata.path("totalTokenCount").asInt(0);
            Integer cachedTokens = usageMetadata.path("cachedContentTokenCount").asInt(0);
            
            String extractedText = extractTextFromResponse(responseBody);
            
            log.info("Cache hit - saved {} tokens, dynamic tokens: {}", cachedTokens, inputTokens);
            
            String combinedPrompt = "[CACHED: " + cachedTokens + " tokens]" + "\n" + 
                                   (dynamicContext != null ? dynamicContext : "") + "\n" + userMessage;
            recordUsage(profileId, userId, stageName, combinedPrompt, extractedText, responseBody, 
                    inputTokens, outputTokens, totalTokens, true, null, latencyMs, true, cachedTokens, inputTokens);
            
            return extractedText;
        } catch (IOException e) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            String combinedPrompt = "[CACHED]" + "\n" + (dynamicContext != null ? dynamicContext : "") + "\n" + userMessage;
            recordUsage(profileId, userId, stageName, combinedPrompt, null, null, null, null, null, 
                    false, e.getMessage(), latencyMs, true, null, null);
            throw e;
        }
    }
    
    /**
     * Generate content with static system prompt (fallback without caching)
     * Use this when caching is not available or for one-off requests
     */
    public String generateContentWithSystemPrompt(String systemPrompt, String dynamicContext, 
                                                  String userMessage, Long profileId, Long userId, 
                                                  String stageName) throws IOException {
        String url = String.format(GEMINI_API_URL, model, apiKey);
        
        String requestBody = buildRequestBodyWithSystemInstruction(systemPrompt, dynamicContext, userMessage);
        long startTime = System.currentTimeMillis();
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            
            if (!response.isSuccessful()) {
                recordUsage(profileId, userId, stageName, systemPrompt + "\n" + dynamicContext + "\n" + userMessage, 
                           null, null, null, null, null, false, "Gemini API error: " + response.code(), latencyMs, false, null, null);
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usageMetadata = root.path("usageMetadata");
            Integer inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
            Integer outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
            Integer totalTokens = usageMetadata.path("totalTokenCount").asInt(0);
            
            String extractedText = extractTextFromResponse(responseBody);
            recordUsage(profileId, userId, stageName, systemPrompt + "\n" + dynamicContext + "\n" + userMessage, 
                       extractedText, responseBody, inputTokens, outputTokens, totalTokens, true, null, latencyMs, false, null, null);
            
            return extractedText;
        } catch (IOException e) {
            long latencyMs = (int) (System.currentTimeMillis() - startTime);
            recordUsage(profileId, userId, stageName, systemPrompt + "\n" + dynamicContext + "\n" + userMessage, 
                       null, null, null, null, null, false, e.getMessage(), latencyMs, false, null, null);
            throw e;
        }
    }
    
    /**
     * Generate content with attachments (images, PDFs)
     * Note: For Gemini 2.5 Flash, we'll use inline data for images
     */
    public String generateContentWithAttachments(String prompt, List<String> attachmentUrls) throws IOException {
        return generateContentWithAttachments(prompt, attachmentUrls, null, null, null);
    }
    
    /**
     * Generate content with attachments and tracking
     */
    public String generateContentWithAttachments(String prompt, List<String> attachmentUrls,
                                                  Long profileId, Long userId, String stageName) throws IOException {
        String url = String.format(GEMINI_API_URL, model, apiKey);
        
        String requestBody = buildRequestBodyWithAttachments(prompt, attachmentUrls);
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        long startTime = System.currentTimeMillis();
        
        try (Response response = client.newCall(request).execute()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            
            if (!response.isSuccessful()) {
                recordUsage(profileId, userId, stageName, prompt, null, null, null, null, null, 
                        false, "Gemini API error: " + response.code(), latencyMs, false, null, null);
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            
            // Extract token counts from response
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usageMetadata = root.path("usageMetadata");
            Integer inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
            Integer outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
            Integer totalTokens = usageMetadata.path("totalTokenCount").asInt(0);
            
            String extractedText = extractTextFromResponse(responseBody);
            
            recordUsage(profileId, userId, stageName, prompt, extractedText, responseBody, 
                    inputTokens, outputTokens, totalTokens, true, null, latencyMs, false, null, null);
            
            return extractedText;
        } catch (IOException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            recordUsage(profileId, userId, stageName, prompt, null, null, null, null, null, 
                    false, e.getMessage(), latencyMs, false, null, null);
            throw e;
        }
    }
    
    private String buildRequestBodyWithAttachments(String prompt, List<String> attachmentUrls) {
        StringBuilder partsJson = new StringBuilder();
        partsJson.append("[{\"text\": \"").append(escapeJson(prompt)).append("\"}");
        
        // For now, we'll add attachment URLs as text references
        // In production, you'd fetch and encode images as base64
        if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
            partsJson.append(",{\"text\": \"Attachment URLs: ");
            partsJson.append(String.join(", ", attachmentUrls));
            partsJson.append("\"}");
        }
        
        partsJson.append("]");
        
        return String.format("""
            {
              "contents": [{
                "parts": %s
              }],
              "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 4096
              }
            }
            """, partsJson.toString());
    }
    
    private String buildRequestBody(String prompt) {
        return String.format("""
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 2048
              }
            }
            """, escapeJson(prompt));
    }
    
    /**
     * Build request body with cached content reference (Industry Best Practice)
     * Uses Gemini Context Caching API to reference pre-cached system instructions
     */
    private String buildRequestBodyWithCachedContent(String cachedContentName, String dynamicContext, String userMessage) {
        StringBuilder userContent = new StringBuilder();
        if (dynamicContext != null && !dynamicContext.isEmpty()) {
            userContent.append(escapeJson(dynamicContext)).append("\\n\\n");
        }
        userContent.append(escapeJson(userMessage));
        
        return String.format("""
            {
              "cachedContent": "%s",
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 2048
              }
            }
            """, cachedContentName, userContent.toString());
    }
    
    /**
     * Build request body with system instruction (without caching)
     * Use this for one-off requests or when caching is not needed
     */
    private String buildRequestBodyWithSystemInstruction(String systemPrompt, String dynamicContext, String userMessage) {
        StringBuilder userContent = new StringBuilder();
        if (dynamicContext != null && !dynamicContext.isEmpty()) {
            userContent.append(escapeJson(dynamicContext)).append("\\n\\n");
        }
        userContent.append(escapeJson(userMessage));
        
        return String.format("""
            {
              "systemInstruction": {
                "parts": [{
                  "text": "%s"
                }]
              },
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 2048
              }
            }
            """, escapeJson(systemPrompt), userContent.toString());
    }
    
    private String extractTextFromResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }
        
        return "";
    }
    
    private String extractTextFromStreamChunk(String chunk) throws IOException {
        JsonNode root = objectMapper.readTree(chunk);
        JsonNode candidates = root.path("candidates");
        
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }
        
        return null;
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Record AI model usage for tracking and debugging
     */
    private void recordUsage(Long profileId, Long userId, String stageName, String inputMessage, 
                            String outputMessage, String rawJsonResponse, 
                            Integer inputTokens, Integer outputTokens, Integer totalTokens,
                            boolean isSuccess, String errorMessage, long latencyMs,
                            Boolean cacheUsed, Integer cachedTokens, Integer dynamicTokens) {
        try {
            AiModelUsage usage = AiModelUsage.builder()
                    .profileId(profileId)
                    .userId(userId != null ? userId : 1L)
                    .modelName(model)
                    .operationType(AiModelUsage.OperationType.STRUCTURED)
                    .stageName(stageName)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .cacheUsed(cacheUsed != null ? cacheUsed : false)
                    .cachedTokens(cachedTokens)
                    .dynamicTokens(dynamicTokens)
                    .inputMessage(inputMessage != null && inputMessage.length() > 10000 
                            ? inputMessage.substring(0, 10000) : inputMessage)
                    .userMessageLength(inputMessage != null ? inputMessage.length() : 0)
                    .outputMessage(outputMessage != null && outputMessage.length() > 10000 
                            ? outputMessage.substring(0, 10000) : outputMessage)
                    .responseLength(outputMessage != null ? outputMessage.length() : 0)
                    .rawJsonResponse(rawJsonResponse != null && rawJsonResponse.length() > 10000 
                            ? rawJsonResponse.substring(0, 10000) : rawJsonResponse)
                    .isSuccess(isSuccess)
                    .errorMessage(errorMessage)
                    .latencyMs((int) latencyMs)
                    .hasAttachments(false)
                    .attachmentCount(0)
                    .build();
            
            aiModelUsageService.recordUsage(usage);
        } catch (Exception e) {
            log.warn("Failed to record AI usage: {}", e.getMessage());
        }
    }
}
