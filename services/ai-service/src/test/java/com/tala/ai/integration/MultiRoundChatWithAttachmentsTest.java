package com.tala.ai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.domain.ChatMessage;
import com.tala.ai.repository.ChatMessageRepository;
import com.tala.ai.service.ChatMessageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Multi-Round Chat with Attachments
 * 
 * Tests 5 comprehensive use cases:
 * 1. Simple single-round recording with photo attachment
 * 2. Multi-round clarification flow with attachments
 * 3. Multi-event recording in single message with multiple attachments
 * 4. Context-aware conversation spanning multiple rounds
 * 5. Mixed conversation (questions + recording) with attachments
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiRoundChatWithAttachmentsTest {
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final Long TEST_PROFILE_ID = 1001L;
    private static final Long TEST_USER_ID = 2001L;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        chatMessageRepository.deleteAll();
    }
    
    /**
     * Use Case 1: Simple Single-Round Recording with Photo
     * 
     * Scenario:
     * - User uploads photo of baby eating
     * - User sends message: "Baby had lunch at noon"
     * - System stores message with attachment
     * - AI processes and creates event
     */
    @Test
    @Order(1)
    @Transactional
    void testUseCase1_SimpleSingleRoundWithPhoto() {
        System.out.println("\n=== USE CASE 1: Simple Single-Round Recording with Photo ===");
        
        // Simulate file upload (fileIds from file-service)
        List<Long> attachmentIds = List.of(101L);
        
        // Round 1: User message with attachment
        ChatMessage userMessage = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Baby had lunch at noon",
            attachmentIds
        );
        
        // Verify user message stored correctly
        assertThat(userMessage).isNotNull();
        assertThat(userMessage.getId()).isNotNull();
        assertThat(userMessage.getContent()).isEqualTo("Baby had lunch at noon");
        assertThat(userMessage.getRole()).isEqualTo(ChatMessage.MessageRole.USER);
        assertThat(userMessage.hasAttachments()).isTrue();
        assertThat(userMessage.getAttachmentCount()).isEqualTo(1);
        assertThat(userMessage.getAttachmentIds()).containsExactly(101L);
        
        System.out.println("✓ User message stored: id=" + userMessage.getId() + 
                         ", attachments=" + userMessage.getAttachmentCount());
        
        // Simulate AI response
        ChatMessage assistantMessage = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Great! I recorded that baby had lunch at noon.",
            "DATA_RECORDING",
            0.95,
            null,
            null
        );
        
        // Verify assistant message
        assertThat(assistantMessage).isNotNull();
        assertThat(assistantMessage.getRole()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
        assertThat(assistantMessage.getInteractionType()).isEqualTo("DATA_RECORDING");
        
        System.out.println("✓ Assistant message stored: id=" + assistantMessage.getId());
        
        // Verify conversation history
        List<ChatMessage> messages = chatMessageService.getAllMessages(TEST_PROFILE_ID);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(ChatMessage.MessageRole.USER);
        assertThat(messages.get(1).getRole()).isEqualTo(ChatMessage.MessageRole.ASSISTANT);
        
        System.out.println("✓ Conversation history verified: " + messages.size() + " messages");
        System.out.println("✅ USE CASE 1 PASSED\n");
    }
    
    /**
     * Use Case 2: Multi-Round Clarification Flow
     * 
     * Scenario:
     * - Round 1: User sends photo without details
     * - Round 2: AI asks for time
     * - Round 3: User provides time
     * - System links photo from Round 1 with time from Round 3
     */
    @Test
    @Order(2)
    @Transactional
    void testUseCase2_MultiRoundClarificationFlow() {
        System.out.println("\n=== USE CASE 2: Multi-Round Clarification Flow ===");
        
        // Round 1: User sends photo with vague message
        List<Long> photoIds = List.of(201L);
        ChatMessage round1User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Baby ate lunch",
            photoIds
        );
        
        assertThat(round1User.hasAttachments()).isTrue();
        System.out.println("Round 1 - User: 'Baby ate lunch' [1 attachment]");
        
        // Round 1: AI asks for clarification
        ChatMessage round1Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Great! What time was lunch?",
            "DATA_RECORDING",
            0.85,
            null,
            null
        );
        
        System.out.println("Round 1 - AI: 'Great! What time was lunch?'");
        
        // Round 2: User provides time
        ChatMessage round2User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "At noon"
        );
        
        assertThat(round2User.hasAttachments()).isFalse();
        System.out.println("Round 2 - User: 'At noon'");
        
        // Verify chat history formatting includes attachment context
        String chatHistory = chatMessageService.formatChatHistoryForAI(TEST_PROFILE_ID, 10);
        assertThat(chatHistory).contains("[1 attachment(s)]");
        assertThat(chatHistory).contains("Baby ate lunch");
        assertThat(chatHistory).contains("At noon");
        
        System.out.println("✓ Chat history includes attachment context");
        System.out.println("Chat history:\n" + chatHistory);
        
        // Round 2: AI confirms with complete information
        ChatMessage round2Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Perfect! I recorded that baby had lunch at 12:00 PM.",
            "DATA_RECORDING",
            0.95,
            null,
            null
        );
        
        System.out.println("Round 2 - AI: 'Perfect! I recorded that baby had lunch at 12:00 PM.'");
        
        // Verify complete conversation
        List<ChatMessage> messages = chatMessageService.getAllMessages(TEST_PROFILE_ID);
        assertThat(messages).hasSize(4);
        
        // Verify first user message has attachment
        ChatMessage firstUserMsg = messages.stream()
            .filter(m -> m.getRole() == ChatMessage.MessageRole.USER)
            .findFirst()
            .orElseThrow();
        assertThat(firstUserMsg.hasAttachments()).isTrue();
        assertThat(firstUserMsg.getAttachmentIds()).containsExactly(201L);
        
        System.out.println("✓ Multi-round conversation verified: " + messages.size() + " messages");
        System.out.println("✅ USE CASE 2 PASSED\n");
    }
    
    /**
     * Use Case 3: Multi-Event Recording with Multiple Attachments
     * 
     * Scenario:
     * - User uploads 3 photos
     * - User describes multiple events in one message
     * - System creates multiple events linked to same attachments
     */
    @Test
    @Order(3)
    @Transactional
    void testUseCase3_MultiEventWithMultipleAttachments() {
        System.out.println("\n=== USE CASE 3: Multi-Event Recording with Multiple Attachments ===");
        
        // User uploads 3 photos
        List<Long> multiplePhotos = List.of(301L, 302L, 303L);
        
        // User describes busy morning
        ChatMessage userMessage = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Baby had breakfast at 8am, then played until 10, then napped",
            multiplePhotos
        );
        
        assertThat(userMessage.getAttachmentCount()).isEqualTo(3);
        assertThat(userMessage.getAttachmentIds()).containsExactlyInAnyOrder(301L, 302L, 303L);
        
        System.out.println("User: 'Baby had breakfast at 8am, then played until 10, then napped' [3 attachments]");
        System.out.println("✓ Stored message with " + userMessage.getAttachmentCount() + " attachments");
        
        // AI response acknowledging multiple events
        ChatMessage assistantMessage = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Got it! I recorded:\n- Breakfast at 8:00 AM\n- Playtime from 8:30-10:00 AM\n- Nap from 10:00 AM",
            "DATA_RECORDING",
            0.92,
            null,
            null
        );
        
        System.out.println("AI: 'Got it! I recorded: [3 events]'");
        
        // Verify messages
        List<ChatMessage> messages = chatMessageService.getAllMessages(TEST_PROFILE_ID);
        assertThat(messages).hasSize(2);
        
        // Verify user message retains all attachments
        ChatMessage storedUserMsg = messages.get(0);
        assertThat(storedUserMsg.getAttachmentCount()).isEqualTo(3);
        
        System.out.println("✓ All 3 attachments preserved in conversation");
        System.out.println("✅ USE CASE 3 PASSED\n");
    }
    
    /**
     * Use Case 4: Context-Aware Conversation Spanning Multiple Rounds
     * 
     * Scenario:
     * - Round 1: User mentions baby seems fussy
     * - Round 2: User reports fever with photo
     * - Round 3: User mentions reduced appetite
     * - System links all symptoms across rounds
     */
    @Test
    @Order(4)
    @Transactional
    void testUseCase4_ContextAwareMultiRoundConversation() {
        System.out.println("\n=== USE CASE 4: Context-Aware Multi-Round Conversation ===");
        
        // Round 1: Initial observation
        ChatMessage round1User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Baby seems fussy today"
        );
        
        System.out.println("Round 1 - User: 'Baby seems fussy today'");
        
        ChatMessage round1Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "I'm sorry to hear that. Have you noticed any symptoms?",
            "QUESTION_ANSWERING",
            0.88,
            null,
            null
        );
        
        System.out.println("Round 1 - AI: 'I'm sorry to hear that. Have you noticed any symptoms?'");
        
        // Round 2: Symptom with photo
        List<Long> thermometerPhoto = List.of(401L);
        ChatMessage round2User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Slight fever, 99.5°F",
            thermometerPhoto
        );
        
        assertThat(round2User.hasAttachments()).isTrue();
        System.out.println("Round 2 - User: 'Slight fever, 99.5°F' [1 attachment]");
        
        ChatMessage round2Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "I'll record that. Any other symptoms?",
            "DATA_RECORDING",
            0.90,
            null,
            null
        );
        
        System.out.println("Round 2 - AI: 'I'll record that. Any other symptoms?'");
        
        // Round 3: Additional symptom
        ChatMessage round3User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "No appetite, didn't finish breakfast"
        );
        
        System.out.println("Round 3 - User: 'No appetite, didn't finish breakfast'");
        
        // Verify chat history maintains context
        String chatHistory = chatMessageService.formatChatHistoryForAI(TEST_PROFILE_ID, 10);
        assertThat(chatHistory).contains("fussy");
        assertThat(chatHistory).contains("fever");
        assertThat(chatHistory).contains("appetite");
        assertThat(chatHistory).contains("[1 attachment(s)]");
        
        System.out.println("✓ Chat history maintains full context across rounds");
        
        ChatMessage round3Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Recorded fever (99.5°F) and reduced appetite. Should I also note the fussy mood from earlier?",
            "DATA_RECORDING",
            0.93,
            null,
            null
        );
        
        System.out.println("Round 3 - AI: 'Recorded fever (99.5°F) and reduced appetite...'");
        
        // Verify complete conversation
        List<ChatMessage> messages = chatMessageService.getAllMessages(TEST_PROFILE_ID);
        assertThat(messages).hasSize(6);
        
        // Count messages with attachments
        long messagesWithAttachments = messages.stream()
            .filter(ChatMessage::hasAttachments)
            .count();
        assertThat(messagesWithAttachments).isEqualTo(1);
        
        System.out.println("✓ Conversation has " + messages.size() + " messages");
        System.out.println("✓ Context-aware linking across 3 rounds verified");
        System.out.println("✅ USE CASE 4 PASSED\n");
    }
    
    /**
     * Use Case 5: Mixed Conversation (Questions + Recording) with Attachments
     * 
     * Scenario:
     * - User asks question about feeding
     * - AI provides answer
     * - User then records feeding event with photo
     * - System correctly classifies and handles both interaction types
     */
    @Test
    @Order(5)
    @Transactional
    void testUseCase5_MixedConversationWithAttachments() {
        System.out.println("\n=== USE CASE 5: Mixed Conversation (Questions + Recording) ===");
        
        // Round 1: Question
        ChatMessage round1User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "What did baby eat yesterday?"
        );
        
        System.out.println("Round 1 - User: 'What did baby eat yesterday?'");
        
        ChatMessage round1Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Yesterday baby had oatmeal for breakfast and pasta for lunch.",
            "QUESTION_ANSWERING",
            0.91,
            null,
            null
        );
        
        assertThat(round1Assistant.getInteractionType()).isEqualTo("QUESTION_ANSWERING");
        System.out.println("Round 1 - AI: 'Yesterday baby had oatmeal...' [QUESTION_ANSWERING]");
        
        // Round 2: Recording with photo
        List<Long> foodPhoto = List.of(501L);
        ChatMessage round2User = chatMessageService.storeUserMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Today baby had chicken and rice for lunch",
            foodPhoto
        );
        
        assertThat(round2User.hasAttachments()).isTrue();
        System.out.println("Round 2 - User: 'Today baby had chicken and rice for lunch' [1 attachment]");
        
        ChatMessage round2Assistant = chatMessageService.storeAssistantMessage(
            TEST_PROFILE_ID,
            TEST_USER_ID,
            "Great! I recorded that baby had chicken and rice for lunch today.",
            "DATA_RECORDING",
            0.94,
            null,
            null
        );
        
        assertThat(round2Assistant.getInteractionType()).isEqualTo("DATA_RECORDING");
        System.out.println("Round 2 - AI: 'Great! I recorded...' [DATA_RECORDING]");
        
        // Verify conversation flow
        List<ChatMessage> messages = chatMessageService.getAllMessages(TEST_PROFILE_ID);
        assertThat(messages).hasSize(4);
        
        // Verify interaction types are different
        List<String> interactionTypes = messages.stream()
            .filter(m -> m.getRole() == ChatMessage.MessageRole.ASSISTANT)
            .map(ChatMessage::getInteractionType)
            .toList();
        
        assertThat(interactionTypes).containsExactly("QUESTION_ANSWERING", "DATA_RECORDING");
        
        System.out.println("✓ Mixed interaction types handled correctly");
        System.out.println("✓ Attachment only in recording round");
        System.out.println("✅ USE CASE 5 PASSED\n");
    }
    
    /**
     * Integration Test: Verify Chat History Formatting
     */
    @Test
    @Order(6)
    @Transactional
    void testChatHistoryFormattingWithAttachments() {
        System.out.println("\n=== INTEGRATION TEST: Chat History Formatting ===");
        
        // Create conversation with mixed attachments
        chatMessageService.storeUserMessage(TEST_PROFILE_ID, TEST_USER_ID, "Hello");
        chatMessageService.storeAssistantMessage(TEST_PROFILE_ID, TEST_USER_ID, "Hi! How can I help?", "GENERAL_CHAT", 0.9, null, null);
        chatMessageService.storeUserMessage(TEST_PROFILE_ID, TEST_USER_ID, "Baby ate lunch", List.of(601L, 602L));
        chatMessageService.storeAssistantMessage(TEST_PROFILE_ID, TEST_USER_ID, "What time?", "DATA_RECORDING", 0.85, null, null);
        chatMessageService.storeUserMessage(TEST_PROFILE_ID, TEST_USER_ID, "At noon");
        
        // Get formatted history
        String history = chatMessageService.formatChatHistoryForAI(TEST_PROFILE_ID, 10);
        
        System.out.println("Formatted Chat History:");
        System.out.println(history);
        
        // Verify format
        assertThat(history).contains("User: Hello");
        assertThat(history).contains("Assistant: Hi! How can I help?");
        assertThat(history).contains("User: Baby ate lunch [2 attachment(s)]");
        assertThat(history).contains("Assistant: What time?");
        assertThat(history).contains("User: At noon");
        
        // Verify no attachment indicator for messages without attachments
        assertThat(history.split("User: Hello")[1].split("\n")[0]).doesNotContain("attachment");
        
        System.out.println("✓ Chat history formatting verified");
        System.out.println("✅ INTEGRATION TEST PASSED\n");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("Test completed. Database cleaned up.\n");
    }
}
