package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.dto.AttachmentParserResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1: Attachment Parser Service
 * 
 * Analyzes attachments (images, PDFs, documents) and extracts:
 * - Content summary
 * - Extracted text (OCR for images)
 * - Key findings
 * - Document type classification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentParserService {
    
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    
    /**
     * Parse attachments and extract content
     * 
     * @param attachmentUrls List of attachment URLs to analyze
     * @param userMessage User's message context
     * @return Parsed attachment result
     */
    public AttachmentParserResult parseAttachments(List<String> attachmentUrls, String userMessage) {
        return parseAttachments(attachmentUrls, userMessage, null, null);
    }
    
    /**
     * Parse attachments with tracking
     */
    public AttachmentParserResult parseAttachments(List<String> attachmentUrls, String userMessage, 
                                                    Long profileId, Long userId) {
        log.info("Parsing {} attachments", attachmentUrls != null ? attachmentUrls.size() : 0);
        
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return AttachmentParserResult.builder()
                    .overallSummary("No attachments provided")
                    .attachments(new ArrayList<>())
                    .confidence(1.0)
                    .build();
        }
        
        try {
            // Build prompt for attachment analysis
            String prompt = buildAttachmentAnalysisPrompt(userMessage);
            
            // Call Gemini with attachments and tracking
            String aiResponse = geminiService.generateContentWithAttachments(prompt, attachmentUrls, 
                    profileId, userId, "AttachmentParser");
            
            // Parse response
            AttachmentParserResult result = parseAttachmentResponse(aiResponse);
            result.setRawAiResponse(aiResponse);
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse attachments", e);
            return AttachmentParserResult.builder()
                    .overallSummary("Failed to analyze attachments: " + e.getMessage())
                    .attachments(new ArrayList<>())
                    .confidence(0.0)
                    .aiThinkProcess("Error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Build prompt for attachment analysis
     */
    private String buildAttachmentAnalysisPrompt(String userMessage) {
        return """
            You are a professional document and image analysis assistant for a baby tracking application.
            Your task is to carefully analyze all attached files and extract their content.
            
            For EACH file, provide:
            1. contentSummary: Brief 1-2 sentence overview
            2. extractedText: Complete text content (OCR for images, text for PDFs)
            3. keyFindings: Array of key points or important information
            4. detectedType: Document type classification
            
            Document types:
            - DAYCARE_REPORT: Daily reports with meals, naps, diaper changes, activities
            - MEDICAL_RECORD: Medical reports, lab results, prescriptions, doctor's notes
            - PHOTO: Photos of babies, family, activities
            - DOCUMENT: General documents, receipts, notes
            - OTHER: Anything else
            
            """ + (userMessage != null && !userMessage.isBlank() 
                ? "\nUser message context: \"" + userMessage + "\"\n" 
                : "") + """
            
            Return ONLY valid JSON in this exact format:
            {
              "overallSummary": "Brief summary of all files combined",
              "attachmentType": "DAYCARE_REPORT|MEDICAL_RECORD|PHOTO|DOCUMENT|OTHER",
              "attachments": [
                {
                  "fileName": "file1.png",
                  "contentSummary": "Brief overview",
                  "extractedText": "Complete extracted text or image description",
                  "keyFindings": ["Finding 1", "Finding 2", "Finding 3"],
                  "confidence": 0.95,
                  "detectedType": "DAYCARE_REPORT"
                }
              ],
              "aiThinkProcess": "Your reasoning",
              "confidence": 0.95
            }
            
            IMPORTANT:
            - For multi-page PDFs, create separate entries for each page
            - extractedText should be comprehensive
            - keyFindings must be an array of strings
            - confidence should be 0.0-1.0
            """;
    }
    
    /**
     * Parse AI response into structured result
     */
    private AttachmentParserResult parseAttachmentResponse(String aiResponse) throws Exception {
        String json = extractJson(aiResponse);
        JsonNode root = objectMapper.readTree(json);
        
        // Parse overall info
        String overallSummary = root.path("overallSummary").asText();
        String attachmentTypeStr = root.path("attachmentType").asText("OTHER");
        AttachmentParserResult.AttachmentType attachmentType = 
                AttachmentParserResult.AttachmentType.valueOf(attachmentTypeStr);
        double confidence = root.path("confidence").asDouble(0.8);
        String aiThinkProcess = root.path("aiThinkProcess").asText("");
        
        // Parse individual attachments
        List<AttachmentParserResult.AttachmentSummary> attachments = new ArrayList<>();
        JsonNode attachmentsNode = root.path("attachments");
        if (attachmentsNode.isArray()) {
            for (JsonNode attachmentNode : attachmentsNode) {
                AttachmentParserResult.AttachmentSummary summary = parseAttachmentSummary(attachmentNode);
                attachments.add(summary);
            }
        }
        
        return AttachmentParserResult.builder()
                .overallSummary(overallSummary)
                .attachmentType(attachmentType)
                .attachments(attachments)
                .confidence(confidence)
                .aiThinkProcess(aiThinkProcess)
                .build();
    }
    
    /**
     * Parse individual attachment summary
     */
    private AttachmentParserResult.AttachmentSummary parseAttachmentSummary(JsonNode node) {
        String fileName = node.path("fileName").asText("");
        String contentSummary = node.path("contentSummary").asText("");
        String extractedText = node.path("extractedText").asText("");
        double confidence = node.path("confidence").asDouble(0.8);
        String detectedTypeStr = node.path("detectedType").asText("OTHER");
        AttachmentParserResult.AttachmentType detectedType = 
                AttachmentParserResult.AttachmentType.valueOf(detectedTypeStr);
        
        // Parse key findings
        List<String> keyFindings = new ArrayList<>();
        JsonNode findingsNode = node.path("keyFindings");
        if (findingsNode.isArray()) {
            for (JsonNode finding : findingsNode) {
                keyFindings.add(finding.asText());
            }
        }
        
        return AttachmentParserResult.AttachmentSummary.builder()
                .fileName(fileName)
                .contentSummary(contentSummary)
                .extractedText(extractedText)
                .keyFindings(keyFindings)
                .confidence(confidence)
                .detectedType(detectedType)
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
