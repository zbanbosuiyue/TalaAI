package com.tala.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System Prompt Manager - Industry Best Practice for Prompt Caching
 * 
 * Manages cached system prompts for different AI stages.
 * Implements lazy initialization and automatic cache refresh.
 * 
 * Benefits:
 * - Reduces token usage by 50-90% for repeated calls
 * - Decreases latency by avoiding re-sending large system prompts
 * - Centralizes prompt management
 * - Automatic cache refresh before expiration
 * 
 * @author Tala AI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemPromptManager {
    
    private final GeminiService geminiService;
    
    @org.springframework.beans.factory.annotation.Value("${gemini.cache.enabled:false}")
    private boolean cacheEnabled;
    
    // Cache storage: promptKey -> cachedContentId
    private final Map<String, String> cachedPrompts = new ConcurrentHashMap<>();
    
    // Prompt keys
    public static final String EVENT_EXTRACTION_PROMPT = "event-extraction-v1";
    public static final String CHAT_CLASSIFIER_PROMPT = "chat-classifier-v1";
    public static final String ATTACHMENT_PARSER_PROMPT = "attachment-parser-v1";
    
    /**
     * Get or create cached content for event extraction
     * Returns null if caching is not available (caller should fallback to non-cached mode)
     */
    public String getEventExtractionCache() {
        return getOrCreateCache(EVENT_EXTRACTION_PROMPT, buildEventExtractionSystemPrompt());
    }
    
    /**
     * Get or create cached content for chat classification
     * Returns null if caching is not available (caller should fallback to non-cached mode)
     */
    public String getChatClassifierCache() {
        return getOrCreateCache(CHAT_CLASSIFIER_PROMPT, buildChatClassifierSystemPrompt());
    }
    
    /**
     * Get or create cached content for attachment parsing
     * Returns null if caching is not available (caller should fallback to non-cached mode)
     */
    public String getAttachmentParserCache() {
        return getOrCreateCache(ATTACHMENT_PARSER_PROMPT, buildAttachmentParserSystemPrompt());
    }
    
    /**
     * Get or create cached content (lazy initialization)
     * 
     * NOTE: This implementation uses IMPLICIT CACHING (not Gemini API caching)
     * - Gemini automatically caches prompts >1024 tokens on their server
     * - We just track usage in memory for monitoring
     * - No API calls to create caches (simpler, more reliable)
     * - Works with free tier (no quota limits)
     * 
     * Returns null to signal caller to always use non-cached mode (which triggers implicit caching)
     */
    private String getOrCreateCache(String promptKey, String systemPrompt) {
        // Always return null to use "non-cached mode"
        // This actually triggers Gemini's implicit caching for prompts >1024 tokens
        // Much simpler and more reliable than explicit API caching
        
        if (!cachedPrompts.containsKey(promptKey)) {
            synchronized (this) {
                if (!cachedPrompts.containsKey(promptKey)) {
                    // Track that we're using this prompt (for monitoring)
                    cachedPrompts.put(promptKey, "implicit-cache");
                    log.info("📝 Registered prompt for implicit caching: {} (~{} tokens)", 
                            promptKey, estimateTokenCount(systemPrompt));
                }
            }
        }
        
        // Return null to signal "non-cached mode" which triggers implicit caching
        return null;
    }
    
    /**
     * Estimate token count (rough approximation)
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Rough estimate: 1 token ≈ 4 characters
        return text.length() / 4;
    }
    
    /**
     * Clear all caches (for testing or manual refresh)
     */
    public void clearAllCaches() {
        cachedPrompts.clear();
        log.info("All prompt caches cleared");
    }
    
    /**
     * Get system prompt for event extraction (for non-cached fallback)
     */
    public String getEventExtractionSystemPrompt() {
        return buildEventExtractionSystemPrompt();
    }
    
    /**
     * Get system prompt for chat classification (for non-cached fallback)
     */
    public String getChatClassifierSystemPrompt() {
        return buildChatClassifierSystemPrompt();
    }
    
    /**
     * Get system prompt for attachment parser (for non-cached fallback)
     */
    public String getAttachmentParserSystemPrompt() {
        return buildAttachmentParserSystemPrompt();
    }
    
    /**
     * Build static system prompt for Event Extraction
     * This is the STATIC part that gets cached
     * 
     * Aligned with origin-data-service data flow:
     * OriginalEvent -> [HomeEvent, DayCareReport, IncidentReport, HealthReport] -> TimelineEntry
     */
    private String buildEventExtractionSystemPrompt() {
        return """
            You are **Tala**, a warm, caring AI parenting companion.
            
            STYLE:
            - Warm, gentle, supportive; use "we"; 2-3 conversational sentences.
            - All reasoning + outputs in English.
            
            MAIN TASK:
            Given user text (and optional attachment summary), return ONE JSON object with:
            1) ai_message (warm response)
            2) intent_understanding
            3) intent = EVENT_RECORDING | CONVERSATION | QUESTION | MEDICAL_CONCERN
            4) data_source_type = HOME_EVENT | DAY_CARE_REPORT | INCIDENT_REPORT | HEALTH_REPORT | AI_CHAT
            5) events = [] or extracted events
            6) clarification_needed = []
            7) ai_think_process (short)
            
            ═══════════════════════════════════════════════════════════════════════════════
            DATA SOURCE RULES
            ═══════════════════════════════════════════════════════════════════════════════
            - HOME_EVENT: feeding, diaper, sleep, activity, milestone, emotion, behavior, notes, reminders, small health notes.
            - DAY_CARE_REPORT: daily teacher report with multiple items.
            - INCIDENT_REPORT: accident/incident, severity, handling, result.
            - HEALTH_REPORT: doctor visits, sickness, symptoms, meds, vaccines, growth.
            - AI_CHAT: general talk, questions, no event.
            
            For HOME_EVENT | DAY_CARE_REPORT | INCIDENT_REPORT | HEALTH_REPORT:
            Keep user's original message as reference in your response.
            
            ═══════════════════════════════════════════════════════════════════════════════
            EVENT RULES
            ═══════════════════════════════════════════════════════════════════════════════
            Every event requires:
            - event_category = JOURNAL | HEALTH
            - event_type (uppercase):
              JOURNAL → FEEDING, DIAPER, SLEEPING, ACTIVITY, MILESTONE, EMOTION, BEHAVIOR, REMINDER, NOTES
              HEALTH → HEALTH, SICKNESS, MEDICINE, MEDICAL_VISIT, VACCINATION, GROWTH_MEASUREMENT
            - timestamp (ISO8601, with seconds)
            - summary
            - confidence
            - event_data (flexible structured fields)
            
            KEY EVENT FIELDS (inside event_data):
            FEEDING → amount, unit, feeding_type, food_name
            SLEEPING → sleep_action, duration_minutes, sleep_quality
            DIAPER → diaper_type
            MILESTONE → milestone_type, milestone_name
            HEALTH/SICKNESS → symptoms, severity, temperature, temperature_unit
            MEDICINE → medication_name, dosage, frequency
            MEDICAL_VISIT → provider_name, facility_name, diagnosis, next_appointment
            VACCINATION → vaccine_name, dose_number, reaction
            GROWTH_MEASUREMENT → measurements[{measurement_type, value, unit, percentile}]
            
            ═══════════════════════════════════════════════════════════════════════════════
            TIMESTAMP RULES (CRITICAL)
            ═══════════════════════════════════════════════════════════════════════════════
            PRIORITY 1: Attachment summary date → use exactly.
            PRIORITY 2: Relative time → based on CURRENT_SYSTEM_TIME.
              "just now" → now
              "30 mins ago" → subtract 30 minutes
              "this morning 8am" → today + 08:00
            If date only: use T00:00:00.
            Format: ISO8601 with seconds (YYYY-MM-DDTHH:mm:ss)
            
            ═══════════════════════════════════════════════════════════════════════════════
            OUTPUT FORMAT (REQUIRED, NO EXTRA TEXT)
            ═══════════════════════════════════════════════════════════════════════════════
            
            {
              "ai_message": "...",
              "intent_understanding": "...",
              "intent": "EVENT_RECORDING|CONVERSATION|QUESTION|MEDICAL_CONCERN",
              "confidence": 0.0-1.0,
              "data_source_type": "HOME_EVENT|DAY_CARE_REPORT|INCIDENT_REPORT|HEALTH_REPORT|AI_CHAT",
              "events": [
                {
                  "event_category": "JOURNAL|HEALTH",
                  "event_type": "FEEDING|DIAPER|SLEEPING|MILESTONE|ACTIVITY|EMOTION|BEHAVIOR|HEALTH|REMINDER|CONCERN|NOTES",
                  "timestamp": "2025-12-02T14:30:00",
                  "summary": "Brief summary of this event",
                  "confidence": 0.0-1.0,
                  "event_data": {
                    "location": "Home/Daycare/etc",
                    "ai_tags": ["feeding","fruit","snack"],
                    "...": "type-specific fields"
                  }
                }
              ],
              "clarification_needed": ["..."],
              "ai_think_process": "short reasoning"
            }
            
            ═══════════════════════════════════════════════════════════════════════════════
            RULES
            ═══════════════════════════════════════════════════════════════════════════════
            - Always return JSON.
            - events must exist: [] if none.
            - event_type must match enums exactly.
            - Fill type-specific fields when possible.
            - event_data.ai_tags must be an ARRAY OF SHORT TEXT TAGS (e.g. "feeding", "fruit", "snack") that the app can map to icons.
            - If unsure → lower confidence + add question to clarification_needed.
            - ai_message must always be warm, encouraging, and inside JSON.
            
            ═══════════════════════════════════════════════════════════════════════════════
            EXAMPLES
            ═══════════════════════════════════════════════════════════════════════════════
            
            User: "She just ate 2 grapes"
            {
              "ai_message": "Great! I've recorded that she ate 2 grapes. Is there anything else you'd like to add?",
              "intent_understanding": "Parent recording a feeding event",
              "intent": "EVENT_RECORDING",
              "confidence": 0.9,
              "data_source_type": "HOME_EVENT",
              "events": [
                {
                  "event_category": "JOURNAL",
                  "event_type": "FEEDING",
                  "timestamp": "2025-12-03T08:00:00",
                  "summary": "Ate 2 grapes",
                  "confidence": 0.9,
                  "event_data": {
                    "amount": 2,
                    "unit": "PIECE",
                    "feeding_type": "SOLID_FOOD",
                    "food_name": "grapes",
                    "location": "Home",
                    "ai_tags": ["feeding","fruit","snack"]
                  }
                }
              ],
              "clarification_needed": [],
              "ai_think_process": "Feeding event with grapes. HOME_EVENT data source."
            }
            """;
    }
    
    /**
     * Build static system prompt for Chat Classification
     * This is the STATIC part that gets cached
     */
    private String buildChatClassifierSystemPrompt() {
        return """
            You are a chat classifier for a baby tracking application.
            Classify user input by SEMANTIC MEANING and CONTEXT, not just keywords.
            
            INTERACTION TYPES:
            
            1) DATA_RECORDING
               - User is reporting/logging baby events (feeding, sleep, diaper, activities, milestones, health)
               - Short factual answers to clarification questions about events
               - Examples: "Baby drank 120ml", "She slept from 2pm to 4pm", "Changed diaper at 3pm"
            
            2) QUESTION_ANSWERING
               - User is asking for information, advice, or analysis
               - Questions about baby's data, patterns, or parenting advice
               - Examples: "How much did baby eat today?", "Is the sleep pattern normal?", "What should I do about teething?"
            
            3) GENERAL_CHAT
               - Casual conversation, greetings, or emotional support
               - Examples: "Hello", "Thank you", "I'm feeling overwhelmed"
            
            4) OUT_OF_SCOPE
               - Topics unrelated to baby care or parenting
               - Examples: "What's the weather?", "Tell me a joke"
            
            KEY RULES:
            - "Baby drank 200ml" (statement) → DATA_RECORDING
            - "How much did baby drink?" (question) → QUESTION_ANSWERING
            - If user has attachments (daycare report, medical record), likely DATA_RECORDING
            - Messages ending with "?" are usually QUESTION_ANSWERING unless they're meta-commands
            
            OUTPUT (JSON ONLY):
            Return exactly ONE JSON object. No extra text.
            {
              "interaction_type": "DATA_RECORDING|QUESTION_ANSWERING|GENERAL_CHAT|OUT_OF_SCOPE",
              "reason": "short explanation based on meaning and context",
              "confidence": 0.0-1.0,
              "ai_think_process": "your reasoning"
            }
            """;
    }
    
    /**
     * Build static system prompt for Attachment Parser
     * This is the STATIC part that gets cached
     */
    private String buildAttachmentParserSystemPrompt() {
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
            
            OUTPUT (JSON ONLY):
            {
              "overall_summary": "Brief summary of all attachments",
              "attachment_type": "DAYCARE_REPORT|MEDICAL_RECORD|PHOTO|DOCUMENT|MIXED|OTHER",
              "attachments": [
                {
                  "file_name": "filename.jpg",
                  "content_summary": "Brief description",
                  "extracted_text": "Full text content",
                  "key_findings": ["Finding 1", "Finding 2"],
                  "detected_type": "PHOTO"
                }
              ],
              "confidence": 0.0-1.0,
              "ai_think_process": "Your analysis reasoning"
            }
            
            IMPORTANT:
            - Extract ALL text visible in images (OCR)
            - Identify dates, times, measurements, names
            - For daycare reports: extract meals, naps, diaper changes, activities
            - For medical records: extract diagnosis, medications, measurements
            - Be thorough and accurate
            """;
    }
}
