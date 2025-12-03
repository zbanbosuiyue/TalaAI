package com.tala.reminder.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.reminder.dto.CreateReminderRequest;
import com.tala.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Kafka consumer for event-driven reminder creation
 * Only enabled when kafka is explicitly enabled in configuration
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EventConsumer {
    
    private final ReminderService reminderService;
    private final ObjectMapper objectMapper;
    
    /**
     * Listen to event topic and create reminders
     */
    @KafkaListener(topics = "tala.events", groupId = "reminder-service")
    public void consumeEvent(String message) {
        log.info("Received event: {}", message);
        
        try {
            // Parse event message
            JsonNode eventNode = objectMapper.readTree(message);
            
            String eventType = eventNode.path("eventType").asText();
            Long userId = eventNode.path("userId").asLong();
            Long profileId = eventNode.path("profileId").asLong();
            Long eventId = eventNode.path("eventId").asLong();
            
            // Check if reminder should be created based on event type
            if (shouldCreateReminder(eventType)) {
                createReminderFromEvent(eventNode, eventType, userId, profileId, eventId);
            } else {
                log.debug("Event type {} does not require reminder creation", eventType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process event message: {}", message, e);
        }
    }
    
    /**
     * Determine if event type should trigger reminder creation
     */
    private boolean shouldCreateReminder(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "INCIDENT", "INJURY", "TEACHER_NOTE", "SICKNESS", 
                 "MEDICATION_DUE", "VACCINATION_DUE", "CHECKUP_DUE" -> true;
            default -> false;
        };
    }
    
    /**
     * Create reminder from event data
     */
    private void createReminderFromEvent(JsonNode eventNode, String eventType, 
                                        Long userId, Long profileId, Long eventId) {
        try {
            String title = extractTitle(eventNode, eventType);
            String description = extractDescription(eventNode, eventType);
            String category = mapEventTypeToCategory(eventType);
            Instant dueAt = calculateDueDate(eventType);
            String priority = determinePriority(eventType);
            
            CreateReminderRequest request = CreateReminderRequest.builder()
                .userId(userId)
                .profileId(profileId)
                .sourceEventId(eventId)
                .category(category)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .priority(priority)
                .build();
            
            reminderService.create(request);
            log.info("Created reminder from event: type={}, eventId={}, userId={}", 
                eventType, eventId, userId);
            
        } catch (Exception e) {
            log.error("Failed to create reminder from event: eventId={}, userId={}", 
                eventId, userId, e);
        }
    }
    
    /**
     * Extract title from event data
     */
    private String extractTitle(JsonNode eventNode, String eventType) {
        String title = eventNode.path("title").asText();
        if (title != null && !title.isEmpty()) {
            return title;
        }
        
        // Generate default title based on event type
        return switch (eventType.toUpperCase()) {
            case "INCIDENT" -> "Follow up on incident";
            case "INJURY" -> "Monitor injury recovery";
            case "TEACHER_NOTE" -> "Review teacher note";
            case "SICKNESS" -> "Check on health status";
            case "MEDICATION_DUE" -> "Medication reminder";
            case "VACCINATION_DUE" -> "Vaccination due";
            case "CHECKUP_DUE" -> "Schedule checkup";
            default -> "Reminder: " + eventType;
        };
    }
    
    /**
     * Extract description from event data
     */
    private String extractDescription(JsonNode eventNode, String eventType) {
        String description = eventNode.path("description").asText();
        if (description != null && !description.isEmpty()) {
            return description;
        }
        
        String summary = eventNode.path("summary").asText();
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }
        
        return "Please review this " + eventType.toLowerCase() + " event";
    }
    
    /**
     * Map event type to reminder category
     */
    private String mapEventTypeToCategory(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "INCIDENT", "INJURY" -> "SAFETY";
            case "TEACHER_NOTE" -> "COMMUNICATION";
            case "SICKNESS", "MEDICATION_DUE" -> "HEALTH";
            case "VACCINATION_DUE", "CHECKUP_DUE" -> "MEDICAL";
            default -> "GENERAL";
        };
    }
    
    /**
     * Calculate due date based on event type
     */
    private Instant calculateDueDate(String eventType) {
        Instant now = Instant.now();
        
        return switch (eventType.toUpperCase()) {
            case "INCIDENT", "INJURY" -> now.plus(2, ChronoUnit.HOURS);
            case "TEACHER_NOTE" -> now.plus(4, ChronoUnit.HOURS);
            case "SICKNESS" -> now.plus(6, ChronoUnit.HOURS);
            case "MEDICATION_DUE" -> now.plus(30, ChronoUnit.MINUTES);
            case "VACCINATION_DUE", "CHECKUP_DUE" -> now.plus(1, ChronoUnit.DAYS);
            default -> now.plus(1, ChronoUnit.DAYS);
        };
    }
    
    /**
     * Determine priority based on event type
     */
    private String determinePriority(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "INCIDENT", "INJURY", "MEDICATION_DUE" -> "HIGH";
            case "SICKNESS", "TEACHER_NOTE" -> "MEDIUM";
            default -> "LOW";
        };
    }
}
