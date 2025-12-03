/**
 * AI Service Package - Multi-Round Chat Conversation with Attachments
 * 
 * <h2>Architecture Overview</h2>
 * This package implements a sophisticated multi-round chat conversation system
 * with multimodal attachment support following industry best practices.
 * 
 * <h2>Multi-Round Conversation Flow</h2>
 * 
 * <h3>Phase 1: User Input Reception</h3>
 * <pre>{@code
 * Frontend → ChatController.chat()
 *   ├─ User message: "Baby had lunch at noon"
 *   ├─ Attachments: [photo1.jpg, photo2.jpg] (uploaded to file-service)
 *   └─ Context: profileId, userId, userLocalTime
 * }</pre>
 * 
 * <h3>Phase 2: Chat Message Storage</h3>
 * <pre>{@code
 * ChatMessageService.storeUserMessage(profileId, userId, content, attachmentIds)
 *   ├─ Store in chat_messages table
 *   ├─ Link attachment_ids (JSONB array)
 *   └─ Return ChatMessage entity
 * }</pre>
 * 
 * <h3>Phase 3: Context Building</h3>
 * <pre>{@code
 * ChatMessageService.formatChatHistoryForAI(profileId, limit=10)
 *   ├─ Retrieve last 10 messages
 *   ├─ Format with attachment indicators
 *   └─ Example output:
 *       "User: What did baby eat today?
 *        Assistant: I can help you record that! What did baby eat?
 *        User: Baby had lunch at noon [2 attachment(s)]"
 * }</pre>
 * 
 * <h3>Phase 4: AI Processing Pipeline</h3>
 * <pre>{@code
 * AIProcessingOrchestrator.processInput(request)
 *   │
 *   ├─ Stage 1: Attachment Parser (if attachments present)
 *   │   └─ AttachmentParserService.parseAttachments()
 *   │       ├─ Analyze images with Gemini Vision
 *   │       ├─ Extract text from documents
 *   │       ├─ Transcribe audio files
 *   │       └─ Return: AttachmentParserResult
 *   │           ├─ attachmentType: "PHOTO", "DOCUMENT", "AUDIO"
 *   │           ├─ overallSummary: "Two photos of baby eating pasta"
 *   │           └─ attachments: [
 *   │               {fileName, contentSummary, extractedText, keyFindings}
 *   │             ]
 *   │
 *   ├─ Stage 2: Chat Classifier
 *   │   └─ ChatClassifierService.classifyChat(userMessage, attachmentContext, chatHistory)
 *   │       ├─ Determine intent: DATA_RECORDING, QUESTION_ANSWERING, GENERAL_CHAT
 *   │       ├─ Consider conversation context
 *   │       └─ Return: ChatClassificationResult
 *   │           ├─ interactionType: DATA_RECORDING
 *   │           ├─ confidence: 0.95
 *   │           └─ classificationReason: "User wants to record feeding event"
 *   │
 *   └─ Stage 3: Event Extraction (only if DATA_RECORDING)
 *       └─ EventExtractionService.extractEvents(userMessage, attachmentContext, 
 *                                                 babyProfile, chatHistory, userLocalTime)
 *           ├─ Build comprehensive prompt with:
 *           │   ├─ User message
 *           │   ├─ Attachment analysis results
 *           │   ├─ Chat history (conversation context)
 *           │   ├─ Baby profile (age, preferences, allergies)
 *           │   └─ User local time (for timestamp inference)
 *           │
 *           ├─ Call Gemini with structured output
 *           └─ Return: EventExtractionResult
 *               ├─ events: [
 *               │   {
 *               │     eventCategory: "JOURNAL",
 *               │     eventType: "FEEDING",
 *               │     timestamp: "2024-12-01T12:00:00",
 *               │     summary: "Baby ate pasta for lunch",
 *               │     eventData: {
 *               │       foodItems: ["pasta", "tomato sauce"],
 *               │       amount: "full bowl",
 *               │       mood: "happy"
 *               │     },
 *               │     confidence: 0.92
 *               │   }
 *               │ ]
 *               ├─ aiMessage: "Great! I recorded that baby had pasta for lunch at noon."
 *               ├─ intentUnderstanding: "User recorded feeding event with photos"
 *               └─ clarificationNeeded: false
 * }</pre>
 * 
 * <h3>Phase 5: Assistant Response Storage</h3>
 * <pre>{@code
 * ChatMessageService.storeAssistantMessage(profileId, userId, aiMessage,
 *                                           interactionType, confidence,
 *                                           extractionResult, thinkingProcess)
 *   ├─ Store AI response in chat_messages
 *   ├─ Link extracted_records_json
 *   ├─ Store metadata and thinking_process
 *   └─ Return ChatMessage entity
 * }</pre>
 * 
 * <h3>Phase 6: Event Persistence</h3>
 * <pre>{@code
 * OriginDataServiceFeignClient.sendChatEvent(chatEventRequest)
 *   ├─ Build ChatEventRequest:
 *   │   ├─ profileId
 *   │   ├─ userMessage
 *   │   ├─ aiMessage
 *   │   ├─ events: [extracted events]
 *   │   └─ attachmentFileIds: [file IDs from user message]
 *   │
 *   └─ POST to origin-data-service /api/v1/chat-events
 *       └─ ChatEventController.createChatEvent()
 *           └─ OriginalEventService.createEvent()
 *               ├─ Create OriginalEvent (event sourcing root)
 *               ├─ Store attachmentIds in JSONB column
 *               ├─ Store rawPayload (entire ChatEventRequest)
 *               └─ Mark for AI processing
 * }</pre>
 * 
 * <h2>Multi-Round Conversation Examples</h2>
 * 
 * <h3>Example 1: Clarification Flow</h3>
 * <pre>{@code
 * Round 1:
 *   User: "Baby ate lunch" [photo.jpg]
 *   AI: "Great! What time was lunch?"
 *   → Classification: DATA_RECORDING (incomplete)
 *   → Extraction: clarificationNeeded=true
 * 
 * Round 2:
 *   User: "At noon"
 *   AI: "Perfect! I recorded that baby had pasta for lunch at 12:00 PM."
 *   → Classification: DATA_RECORDING (complete)
 *   → Extraction: Uses photo from Round 1 + time from Round 2
 *   → Creates OriginalEvent with attachmentIds from Round 1
 * }</pre>
 * 
 * <h3>Example 2: Multi-Event Recording</h3>
 * <pre>{@code
 * Round 1:
 *   User: "Baby had a busy morning"
 *   AI: "Tell me about it! What happened?"
 * 
 * Round 2:
 *   User: "Breakfast at 8am, then played until 10, then napped" [2 photos]
 *   AI: "Got it! I recorded:
 *        - Breakfast at 8:00 AM
 *        - Playtime from 8:30-10:00 AM
 *        - Nap from 10:00 AM"
 *   → Extraction: Creates 3 events from single message
 *   → All events link to same OriginalEvent with attachments
 * }</pre>
 * 
 * <h3>Example 3: Context-Aware Understanding</h3>
 * <pre>{@code
 * Round 1:
 *   User: "Baby seems fussy today"
 *   AI: "I'm sorry to hear that. Have you noticed any symptoms?"
 * 
 * Round 2:
 *   User: "Slight fever, 99.5°F"
 *   AI: "I'll record that. Any other symptoms?"
 * 
 * Round 3:
 *   User: "No appetite, didn't finish breakfast"
 *   AI: "Recorded fever (99.5°F) and reduced appetite. 
 *        Should I also note the fussy mood from earlier?"
 *   → Context awareness: Links symptoms across multiple rounds
 *   → Creates comprehensive health record
 * }</pre>
 * 
 * <h2>Attachment Handling Best Practices</h2>
 * 
 * <h3>1. Attachment Storage Strategy</h3>
 * <ul>
 *   <li>User uploads → file-service → returns fileIds</li>
 *   <li>ChatMessage stores fileIds in attachment_ids column</li>
 *   <li>OriginalEvent stores same fileIds (single source of truth)</li>
 *   <li>Child entities (HomeEvent, etc.) reference OriginalEvent</li>
 * </ul>
 * 
 * <h3>2. Attachment Context in AI Prompts</h3>
 * <pre>{@code
 * Prompt Structure:
 * 
 * SYSTEM: You are a baby tracking assistant...
 * 
 * CONVERSATION HISTORY:
 * User: What did baby eat today?
 * Assistant: I can help you record that!
 * User: Baby had lunch [2 attachment(s)]
 * 
 * ATTACHMENT ANALYSIS:
 * Type: PHOTO
 * Overall: Two photos of baby eating pasta with tomato sauce
 * 
 * File: photo1.jpg
 * Summary: Baby sitting in high chair with bowl of pasta
 * Key Findings:
 *   - Food type: Pasta with red sauce
 *   - Baby appears happy and engaged
 *   - Clean bib suggests start of meal
 * 
 * File: photo2.jpg
 * Summary: Close-up of pasta bowl, mostly empty
 * Key Findings:
 *   - Bowl nearly finished
 *   - Indicates good appetite
 *   - Timestamp suggests 15 minutes later
 * 
 * BABY PROFILE:
 * Name: Emma
 * Age: 14 months
 * Allergies: None
 * Preferences: Loves pasta
 * 
 * USER LOCAL TIME: 2024-12-01 12:30:00 PST
 * 
 * CURRENT USER MESSAGE: "at noon"
 * 
 * TASK: Extract structured event data...
 * }</pre>
 * 
 * <h3>3. Attachment Persistence Flow</h3>
 * <pre>{@code
 * 1. Frontend uploads files → file-service
 *    └─ Returns: [fileId1, fileId2]
 * 
 * 2. Frontend sends chat message with fileIds
 *    └─ ChatController receives: {message, attachmentIds: [fileId1, fileId2]}
 * 
 * 3. ChatMessageService stores user message
 *    └─ ChatMessage.attachmentIds = [fileId1, fileId2]
 * 
 * 4. AI processes with attachment context
 *    └─ AttachmentParserService fetches files and analyzes
 * 
 * 5. ChatEventRequest sent to origin-data-service
 *    └─ attachmentFileIds: [fileId1, fileId2]
 * 
 * 6. OriginalEvent created
 *    └─ OriginalEvent.attachmentIds = [fileId1, fileId2]
 * 
 * 7. Timeline display
 *    └─ AttachmentResolverService.resolve(originalEvent.attachmentIds)
 *        └─ Returns: List<AttachmentRef> with URLs
 * }</pre>
 * 
 * <h2>Error Handling & Edge Cases</h2>
 * 
 * <h3>1. Missing Attachment Files</h3>
 * <ul>
 *   <li>AttachmentResolverService logs warning and continues</li>
 *   <li>Returns partial list of available attachments</li>
 *   <li>Does not fail entire request</li>
 * </ul>
 * 
 * <h3>2. Ambiguous User Intent</h3>
 * <ul>
 *   <li>ChatClassifier returns low confidence score</li>
 *   <li>AI asks clarifying questions</li>
 *   <li>Next round uses accumulated context</li>
 * </ul>
 * 
 * <h3>3. Incomplete Event Data</h3>
 * <ul>
 *   <li>EventExtraction sets clarificationNeeded=true</li>
 *   <li>AI prompts for missing required fields</li>
 *   <li>Stores partial data in metadata for next round</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * 
 * <h3>1. Chat History Limit</h3>
 * <ul>
 *   <li>Default: Last 10 messages (configurable)</li>
 *   <li>Balances context vs. token usage</li>
 *   <li>Older messages summarized if needed</li>
 * </ul>
 * 
 * <h3>2. Attachment Processing</h3>
 * <ul>
 *   <li>Parallel processing for multiple files</li>
 *   <li>Caching of parsed results</li>
 *   <li>Timeout protection (30s per file)</li>
 * </ul>
 * 
 * <h3>3. AI Model Selection</h3>
 * <ul>
 *   <li>Gemini 2.0 Flash for speed + multimodal</li>
 *   <li>Structured output for reliability</li>
 *   <li>Token optimization via prompt engineering</li>
 * </ul>
 * 
 * @author Tala AI Team
 * @version 2.0
 * @since 2024-12-01
 */
package com.tala.ai.service;
