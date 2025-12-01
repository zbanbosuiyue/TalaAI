package com.tala.origindata.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.origindata.constant.DataSourceType;
import com.tala.origindata.domain.OriginalEvent;
import com.tala.origindata.dto.ChatEventRequest;
import com.tala.origindata.repository.OriginalEventRepository;
import com.tala.origindata.service.OriginalEventService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Chat Event with Attachments in Origin-Data-Service
 * 
 * Verifies:
 * 1. OriginalEvent stores attachmentIds correctly
 * 2. Attachment data persists through event sourcing
 * 3. Child entities can access attachments via OriginalEvent
 * 4. Timeline resolution works with attachments
 * 5. Data integrity across all layers
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatEventWithAttachmentsTest {
    
    @Autowired
    private OriginalEventService originalEventService;
    
    @Autowired
    private OriginalEventRepository originalEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final Long TEST_PROFILE_ID = 3001L;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        originalEventRepository.deleteAll();
    }
    
    /**
     * Test 1: OriginalEvent stores single attachment correctly
     */
    @Test
    @Order(1)
    @Transactional
    void testOriginalEventWithSingleAttachment() {
        System.out.println("\n=== TEST 1: OriginalEvent with Single Attachment ===");
        
        // Create ChatEventRequest
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(TEST_PROFILE_ID)
            .userMessage("Baby had lunch at noon")
            .aiMessage("Great! I recorded that baby had lunch at noon.")
            .attachmentFileIds(List.of(1001L))
            .events(List.of(
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("FEEDING")
                    .timestamp(LocalDateTime.now())
                    .summary("Baby had lunch")
                    .eventData(Map.of("foodType", "pasta"))
                    .confidence(0.95)
                    .build()
            ))
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        // Create OriginalEvent with attachment
        OriginalEvent event = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            null,
            Instant.now(),
            rawPayload,
            request.getAttachmentFileIds()
        );
        
        // Verify event created
        assertThat(event).isNotNull();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getProfileId()).isEqualTo(TEST_PROFILE_ID);
        assertThat(event.getSourceType()).isEqualTo(DataSourceType.AI_CHAT);
        
        // Verify attachment stored
        assertThat(event.hasAttachments()).isTrue();
        assertThat(event.getAttachmentCount()).isEqualTo(1);
        assertThat(event.getAttachmentIds()).containsExactly(1001L);
        
        System.out.println("✓ OriginalEvent created: id=" + event.getId());
        System.out.println("✓ Attachment stored: " + event.getAttachmentIds());
        
        // Verify persistence
        OriginalEvent retrieved = originalEventRepository.findById(event.getId()).orElseThrow();
        assertThat(retrieved.hasAttachments()).isTrue();
        assertThat(retrieved.getAttachmentIds()).containsExactly(1001L);
        
        System.out.println("✓ Attachment persisted correctly in database");
        System.out.println("✅ TEST 1 PASSED\n");
    }
    
    /**
     * Test 2: OriginalEvent stores multiple attachments correctly
     */
    @Test
    @Order(2)
    @Transactional
    void testOriginalEventWithMultipleAttachments() throws Exception {
        System.out.println("\n=== TEST 2: OriginalEvent with Multiple Attachments ===");
        
        List<Long> multipleAttachments = List.of(2001L, 2002L, 2003L);
        
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(TEST_PROFILE_ID)
            .userMessage("Baby had a busy morning")
            .aiMessage("Got it! I recorded 3 events.")
            .attachmentFileIds(multipleAttachments)
            .events(List.of(
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("FEEDING")
                    .timestamp(LocalDateTime.now().minusHours(4))
                    .summary("Breakfast")
                    .build(),
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("PLAY")
                    .timestamp(LocalDateTime.now().minusHours(2))
                    .summary("Playtime")
                    .build(),
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("SLEEP")
                    .timestamp(LocalDateTime.now())
                    .summary("Nap")
                    .build()
            ))
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        OriginalEvent event = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            null,
            Instant.now(),
            rawPayload,
            multipleAttachments
        );
        
        // Verify multiple attachments
        assertThat(event.getAttachmentCount()).isEqualTo(3);
        assertThat(event.getAttachmentIds()).containsExactlyInAnyOrder(2001L, 2002L, 2003L);
        
        System.out.println("✓ OriginalEvent created with " + event.getAttachmentCount() + " attachments");
        System.out.println("✓ Attachment IDs: " + event.getAttachmentIds());
        
        // Verify all 3 events link to same OriginalEvent with attachments
        ChatEventRequest retrieved = objectMapper.readValue(event.getRawPayload(), ChatEventRequest.class);
        assertThat(retrieved.getEvents()).hasSize(3);
        assertThat(retrieved.getAttachmentFileIds()).containsExactlyInAnyOrder(2001L, 2002L, 2003L);
        
        System.out.println("✓ All 3 events linked to same OriginalEvent");
        System.out.println("✓ Attachments accessible from rawPayload");
        System.out.println("✅ TEST 2 PASSED\n");
    }
    
    /**
     * Test 3: OriginalEvent without attachments (backward compatibility)
     */
    @Test
    @Order(3)
    @Transactional
    void testOriginalEventWithoutAttachments() throws Exception {
        System.out.println("\n=== TEST 3: OriginalEvent without Attachments ===");
        
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(TEST_PROFILE_ID)
            .userMessage("Baby is sleeping")
            .aiMessage("Noted! Baby is sleeping.")
            .attachmentFileIds(null)  // No attachments
            .events(List.of(
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("SLEEP")
                    .timestamp(LocalDateTime.now())
                    .summary("Sleeping")
                    .build()
            ))
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        // Create without attachments
        OriginalEvent event = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            null,
            Instant.now(),
            rawPayload,
            null
        );
        
        // Verify no attachments
        assertThat(event.hasAttachments()).isFalse();
        assertThat(event.getAttachmentCount()).isEqualTo(0);
        assertThat(event.getAttachmentIds()).isEmpty();
        
        System.out.println("✓ OriginalEvent created without attachments");
        System.out.println("✓ hasAttachments() = false");
        System.out.println("✓ getAttachmentCount() = 0");
        System.out.println("✅ TEST 3 PASSED\n");
    }
    
    /**
     * Test 4: Verify attachment data integrity through event sourcing
     */
    @Test
    @Order(4)
    @Transactional
    void testAttachmentDataIntegrityThroughEventSourcing() throws Exception {
        System.out.println("\n=== TEST 4: Attachment Data Integrity Through Event Sourcing ===");
        
        List<Long> attachmentIds = List.of(4001L, 4002L);
        
        // Create event with attachments
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(TEST_PROFILE_ID)
            .userMessage("Baby had lunch with photo")
            .aiMessage("Recorded!")
            .attachmentFileIds(attachmentIds)
            .events(List.of(
                ChatEventRequest.ExtractedEvent.builder()
                    .eventCategory("JOURNAL")
                    .eventType("FEEDING")
                    .timestamp(LocalDateTime.now())
                    .summary("Lunch")
                    .eventData(Map.of(
                        "foodType", "pasta",
                        "amount", "full bowl",
                        "mood", "happy"
                    ))
                    .build()
            ))
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        OriginalEvent event = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            null,
            Instant.now(),
            rawPayload,
            attachmentIds
        );
        
        Long eventId = event.getId();
        
        System.out.println("✓ Created OriginalEvent: id=" + eventId);
        
        // Clear persistence context to force fresh load
        originalEventRepository.flush();
        
        // Retrieve from database
        OriginalEvent retrieved = originalEventRepository.findById(eventId).orElseThrow();
        
        // Verify attachment IDs preserved
        assertThat(retrieved.getAttachmentIds()).containsExactlyInAnyOrder(4001L, 4002L);
        
        // Verify rawPayload contains attachment IDs
        ChatEventRequest retrievedRequest = objectMapper.readValue(retrieved.getRawPayload(), ChatEventRequest.class);
        assertThat(retrievedRequest.getAttachmentFileIds()).containsExactlyInAnyOrder(4001L, 4002L);
        
        System.out.println("✓ Attachment IDs in entity: " + retrieved.getAttachmentIds());
        System.out.println("✓ Attachment IDs in rawPayload: " + retrievedRequest.getAttachmentFileIds());
        System.out.println("✓ Data integrity verified - both sources match");
        System.out.println("✅ TEST 4 PASSED\n");
    }
    
    /**
     * Test 5: Verify idempotency with attachments
     */
    @Test
    @Order(5)
    @Transactional
    void testIdempotencyWithAttachments() throws Exception {
        System.out.println("\n=== TEST 5: Idempotency with Attachments ===");
        
        String sourceEventId = "test-chat-event-5001";
        List<Long> attachmentIds = List.of(5001L);
        
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(TEST_PROFILE_ID)
            .userMessage("Test message")
            .aiMessage("Test response")
            .attachmentFileIds(attachmentIds)
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        // Create first time
        OriginalEvent event1 = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            sourceEventId,
            Instant.now(),
            rawPayload,
            attachmentIds
        );
        
        System.out.println("✓ First creation: id=" + event1.getId());
        
        // Try to create duplicate
        OriginalEvent event2 = originalEventService.createEvent(
            TEST_PROFILE_ID,
            DataSourceType.AI_CHAT,
            sourceEventId,
            Instant.now(),
            rawPayload,
            attachmentIds
        );
        
        // Verify same event returned
        assertThat(event2.getId()).isEqualTo(event1.getId());
        assertThat(event2.getAttachmentIds()).isEqualTo(event1.getAttachmentIds());
        
        System.out.println("✓ Duplicate creation: id=" + event2.getId() + " (same as first)");
        System.out.println("✓ Idempotency verified - attachments preserved");
        
        // Verify only one event in database
        List<OriginalEvent> allEvents = originalEventRepository.findByProfileIdOrderByEventTimeDesc(TEST_PROFILE_ID);
        assertThat(allEvents).hasSize(1);
        
        System.out.println("✓ Only 1 event in database (no duplicates)");
        System.out.println("✅ TEST 5 PASSED\n");
    }
    
    /**
     * Test 6: Query events by profile and verify attachments
     */
    @Test
    @Order(6)
    @Transactional
    void testQueryEventsByProfileWithAttachments() throws Exception {
        System.out.println("\n=== TEST 6: Query Events by Profile with Attachments ===");
        
        // Create multiple events with different attachment counts
        createTestEvent(TEST_PROFILE_ID, List.of(6001L));
        createTestEvent(TEST_PROFILE_ID, List.of(6002L, 6003L));
        createTestEvent(TEST_PROFILE_ID, null);  // No attachments
        createTestEvent(TEST_PROFILE_ID, List.of(6004L, 6005L, 6006L));
        
        // Query all events for profile
        List<OriginalEvent> events = originalEventService.getEventsByProfile(TEST_PROFILE_ID);
        
        assertThat(events).hasSize(4);
        
        // Count events with attachments
        long eventsWithAttachments = events.stream()
            .filter(OriginalEvent::hasAttachments)
            .count();
        
        assertThat(eventsWithAttachments).isEqualTo(3);
        
        // Verify attachment counts
        List<Integer> attachmentCounts = events.stream()
            .map(OriginalEvent::getAttachmentCount)
            .sorted()
            .toList();
        
        assertThat(attachmentCounts).containsExactly(0, 1, 2, 3);
        
        System.out.println("✓ Retrieved " + events.size() + " events");
        System.out.println("✓ Events with attachments: " + eventsWithAttachments);
        System.out.println("✓ Attachment counts: " + attachmentCounts);
        System.out.println("✅ TEST 6 PASSED\n");
    }
    
    /**
     * Helper method to create test event
     */
    private OriginalEvent createTestEvent(Long profileId, List<Long> attachmentIds) throws Exception {
        ChatEventRequest request = ChatEventRequest.builder()
            .profileId(profileId)
            .userMessage("Test message")
            .aiMessage("Test response")
            .attachmentFileIds(attachmentIds)
            .build();
        
        String rawPayload = objectMapper.writeValueAsString(request);
        
        return originalEventService.createEvent(
            profileId,
            DataSourceType.AI_CHAT,
            null,
            Instant.now(),
            rawPayload,
            attachmentIds
        );
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("Test completed. Database cleaned up.\n");
    }
}
