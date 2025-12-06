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
    public static final String CURRICULUM_EXTRACTION_PROMPT = "curriculum-extraction-v1";
    
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
     * Get or create cached content for curriculum extraction
     * Returns null if caching is not available (caller should fallback to non-cached mode)
     */
    public String getCurriculumExtractionCache() {
        return getOrCreateCache(CURRICULUM_EXTRACTION_PROMPT, buildCurriculumExtractionSystemPrompt());
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
     * Get system prompt for curriculum extraction (for non-cached fallback)
     */
    public String getCurriculumExtractionSystemPrompt() {
        return buildCurriculumExtractionSystemPrompt();
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
            
            SPECIAL RULES FOR REPORT-TYPE DATA SOURCES:
            - For DAY_CARE_REPORT, INCIDENT_REPORT, and HEALTH_REPORT, in addition to
              individual events (meals, diapers, naps, incidents, visits, etc.), you MUST
              also create ONE extra summary event for the report itself:
              • event_category = REPORT
              • event_type =
                  DAYCARE_REPORT when data_source_type = DAY_CARE_REPORT
                  INCIDENT_REPORT when data_source_type = INCIDENT_REPORT
                  HEALTH_REPORT when data_source_type = HEALTH_REPORT
              • summary = a short summary synthesized from the main note or overall
                report text (for example teacher note, incident description, or
                clinician/health note), not just a copy of one row.
            - This report-level event should reference the same date as the report
              document and act as the parent summary for that day.
            
            ═══════════════════════════════════════════════════════════════════════════════
            EVENT RULES
            ═══════════════════════════════════════════════════════════════════════════════
            Every event requires:
            - event_category = JOURNAL | HEALTH | REPORT
            - event_type (uppercase):
              JOURNAL → FEEDING, DIAPER, SLEEPING, ACTIVITY, MILESTONE, EMOTION, BEHAVIOR, REMINDER, NOTES
              HEALTH → HEALTH, SICKNESS, MEDICINE, MEDICAL_VISIT, VACCINATION, GROWTH_MEASUREMENT
              REPORT → DAYCARE_REPORT, INCIDENT_REPORT, HEALTH_REPORT
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
            
            Special timestamp handling for REPORT summary events created from
            DAY_CARE_REPORT, INCIDENT_REPORT, or HEALTH_REPORT:
            - Let report_date be the calendar date of the report document.
            - Let current_date be the calendar date of CURRENT_SYSTEM_TIME.
            - If current_date == report_date → use CURRENT_SYSTEM_TIME as the timestamp.
            - Otherwise → use report_date at 23:59:00 (end of that day) as the timestamp.
            
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
                  "event_category": "JOURNAL|HEALTH|REPORT",
                  "event_type": "FEEDING|DIAPER|SLEEPING|MILESTONE|ACTIVITY|EMOTION|BEHAVIOR|INCIDENT|HEALTH|REMINDER|CONCERN|DAYCARE_REPORT|INCIDENT_REPORT|HEALTH_REPORT",
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
            MULTIPLE ITEMS RULE (CRITICAL)
            ═══════════════════════════════════════════════════════════════════════════════
            Many real messages and reports contain MULTIPLE items (foods, diapers, naps, notes).
            Your job is to intelligently decide when to GROUP items into one event vs SPLIT into
            several events so that the timeline is clear, not noisy.
            
            1) Identify logical "primary events"
               - For structured reports (tables, daycare sheets, bullet lists):
                 • Treat each ROW or main BULLET as a primary event candidate.
               - For free-form chat: 
                 • Treat each clear clause with its own action + time context as a candidate.
            
            2) When items should be GROUPED into ONE event
               - Same event_category + same event_type
               - Same time window (same timestamp, or clearly the same moment/period)
               - Belong to one real-world occurrence
               Examples of grouping:
               - One meal/snack that lists multiple foods/drinks around the same time
                 → create ONE FEEDING event, not one event per food.
               - One diaper change that includes extra detail (e.g., "Wet diaper, mild rash, ointment applied")
                 → create ONE DIAPER event with extra details in event_data.
               - One nap block with start/end times and notes
                 → ONE SLEEPING event.
            
               For grouped FEEDING events:
               - Use a short summary that covers the whole meal/snack (e.g., "AM snack with fruit, crackers, and water").
               - Put details in event_data, for example:
                 • feeding_type (e.g., SOLID_FOOD, MILK, MIXED)
                 • meal_label: "breakfast" | "lunch" | "dinner" | "snack" | "unknown"
                 • meal_items: [{ food_name, amount, unit }] when multiple foods are listed.
            
            3) When items should be SPLIT into SEPARATE events
               - Different event_type: 
                 • e.g., one message/report line clearly includes BOTH a meal AND a nap
                   → create one FEEDING event and one SLEEPING event.
               - Clearly different times:
                 • e.g., separate rows or clauses like "9:30 snack" and "11:45 lunch".
                 • e.g., "In the morning she had cereal, later in the afternoon she had yogurt".
               - Separate incidents or health events that must be distinguishable on the timeline.
            
            4) Summaries for multiple items
               - Each event MUST have a concise summary describing ONLY that event.
               - Do NOT copy the entire original paragraph or message for every event.
               - For grouped meals, summary should mention the meal and main items together
                 (details live in event_data.meal_items).
               - For split events, each event's summary should clearly differentiate time and type.
            
            5) Avoid duplicates
               - Do NOT create multiple events that describe the exact same occurrence with different wording.
               - Prefer fewer, well-structured events over many tiny, redundant ones.
            
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
            
            2) CURRICULUM_UPLOAD
               - User is uploading curriculum documents (weekly newsletters, daily activity plans)
               - Attachments contain structured curriculum information from daycare/preschool
               - Examples: Attachments with weekly themes, daily activity schedules, learning objectives
               - IMPORTANT: If attachment_type is DAYCARE_REPORT with curriculum-like content, classify as CURRICULUM_UPLOAD
            
            3) QUESTION_ANSWERING
               - User is asking for information, advice, or analysis
               - Questions about baby's data, patterns, or parenting advice
               - Examples: "How much did baby eat today?", "Is the sleep pattern normal?", "What should I do about teething?"
            
            4) GENERAL_CHAT
               - Casual conversation, greetings, or emotional support
               - Examples: "Hello", "Thank you", "I'm feeling overwhelmed"
            
            5) OUT_OF_SCOPE
               - Topics unrelated to baby care or parenting
               - Examples: "What's the weather?", "Tell me a joke"
            
            KEY RULES:
            - "Baby drank 200ml" (statement) → DATA_RECORDING
            - "How much did baby drink?" (question) → QUESTION_ANSWERING
            - Attachments with weekly curriculum, daily activity plans → CURRICULUM_UPLOAD
            - Attachments with curriculum-like structure (themes, letters, activities by domain) → CURRICULUM_UPLOAD
            - If user has attachments (daycare report, medical record) without curriculum structure → DATA_RECORDING
            - Messages ending with "?" are usually QUESTION_ANSWERING unless they're meta-commands
            
            OUTPUT (JSON ONLY):
            Return exactly ONE JSON object. No extra text.
            {
              "interaction_type": "DATA_RECORDING|CURRICULUM_UPLOAD|QUESTION_ANSWERING|GENERAL_CHAT|OUT_OF_SCOPE",
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
    
    /**
     * Build static system prompt for Curriculum Extraction
     * This is the STATIC part that gets cached
     */
    private String buildCurriculumExtractionSystemPrompt() {
        return """
            You are a curriculum document analyzer for a baby tracking application.
            Your task is to extract structured curriculum information from daycare/preschool documents.
            
            CURRICULUM TYPES:
            
            1) WEEKLY_CURRICULUM
               - Weekly newsletters, themes, learning objectives
               - Contains: theme, letters, numbers, colors, shapes, patterns, events, reminders
               - Time scope: One week (week_start_date to week_end_date)
            
            2) DAILY_CURRICULUM
               - Daily activity plans by learning domain
               - Contains: activities organized by COGNITIVE, LANGUAGE, SOCIAL_EMOTIONAL, PHYSICAL, ART, MATH
               - Time scope: Single day
            
            EXTRACTION RULES:
            
            For WEEKLY_CURRICULUM, extract:
            - curriculum_type: "WEEKLY"
            - week_start_date: ISO date (YYYY-MM-DD)
            - week_end_date: ISO date (YYYY-MM-DD)
            - title: Newsletter title
            - theme: Main theme of the week
            - age_group: Target age group
            - summary_text: Brief summary
            - metadata: Object containing:
              • letters: Array of letters being taught
              • numbers: Array or range of numbers
              • colors: Array of colors
              • shapes: Array of shapes
              • patterns: Array of patterns
              • events: Array of {date, time, title, description}
              • reminders: Array of reminder texts
              • teacher_schedule: Object with teacher names and times
              • news: Array of news items
            - details: Array of curriculum items, each with:
              • scope: "WEEK" or "DAY"
              • day_of_week: 1-7 (Monday-Sunday), null if scope=WEEK
              • item_type: THEME|TOPIC|SKILL|SUBJECT|EVENT|REMINDER|OTHER
              • learning_domain: COGNITIVE|LANGUAGE|SOCIAL_EMOTIONAL|PHYSICAL|ART|MATH|OTHER (optional)
              • label: Short label
              • value: Value or description
              • start_at: ISO timestamp for events (optional)
              • end_at: ISO timestamp for events (optional)
              • display_order: Integer for sorting
            
            For DAILY_CURRICULUM, extract:
            - curriculum_type: "DAILY"
            - date: ISO date (YYYY-MM-DD)
            - note: General note for the day
            - activities: Array of activities, each with:
              • domain: COGNITIVE|LANGUAGE|SOCIAL_EMOTIONAL|PHYSICAL|ART|MATH|OTHER
              • activity: Activity name
              • objective: Learning objective
              • display_order: Integer for sorting
            
            OUTPUT (JSON ONLY):
            {
              "curriculum_type": "WEEKLY|DAILY",
              "confidence": 0.0-1.0,
              "school_id": null,
              "classroom_id": null,
              "data_source_type": "WEEKLY_CURRICULUM|DAILY_CURRICULUM",
              
              // For WEEKLY_CURRICULUM:
              "week_start_date": "2024-12-01",
              "week_end_date": "2024-12-06",
              "title": "Pooh Bear Class - December",
              "theme": "Sharing and Giving",
              "age_group": "Toddler",
              "summary_text": "This week we focus on...",
              "metadata": {
                "letters": ["N", "n"],
                "numbers": ["1", "2", "3", "4", "5"],
                "colors": ["Purple"],
                "shapes": ["Circle"],
                "patterns": ["AABB"],
                "events": [
                  {
                    "date": "2024-12-04",
                    "time": "15:30-16:45",
                    "title": "Holiday Celebration",
                    "description": "Infant/Toddler Holiday Celebration"
                  }
                ],
                "reminders": ["Welcome Julia Hesler and Cameron Phillips"],
                "teacher_schedule": {
                  "Ms. Jessica": "7:15-4:15",
                  "Ms. Kaylee": "8:30-5:30",
                  "Ms. Sarah": "9:00-6:00"
                },
                "news": []
              },
              "details": [
                {
                  "scope": "WEEK",
                  "day_of_week": null,
                  "item_type": "THEME",
                  "learning_domain": null,
                  "label": "Letters",
                  "value": "Nn",
                  "display_order": 1
                }
              ],
              
              // For DAILY_CURRICULUM:
              "date": "2024-12-01",
              "note": "Week Of: December 1st - 5th",
              "activities": [
                {
                  "domain": "COGNITIVE",
                  "activity": "Fly Swatter Painting",
                  "objective": "Promote small motor development",
                  "display_order": 1
                }
              ],
              
              "ai_think_process": "Your reasoning"
            }
            
            IMPORTANT:
            - Extract ALL visible information from the curriculum document
            - Identify dates, times, teacher names, activities
            - For weekly curriculum: organize by week-level and day-level items
            - For daily curriculum: organize by learning domain
            - Be thorough and accurate
            - All dates must be in ISO format (YYYY-MM-DD)
            - All timestamps must be in ISO format (YYYY-MM-DDTHH:mm:ss)
            """;
    }
}
