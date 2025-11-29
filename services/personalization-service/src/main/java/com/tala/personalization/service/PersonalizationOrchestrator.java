package com.tala.personalization.service;

import com.tala.personalization.dto.PersonalizationContext;
import com.tala.personalization.dto.TodayPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core orchestration service for personalization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationOrchestrator {
    
    private final ContextBuilder contextBuilder;
    private final PriorityCalculator priorityCalculator;
    private final UrgencyCalculator urgencyCalculator;
    
    @Value("${personalization.today.max-pill-topics:5}")
    private int maxPillTopics;
    
    @Value("${personalization.today.min-pill-topics:3}")
    private int minPillTopics;
    
    private static final List<String> ALL_TOPICS = Arrays.asList(
        "sleep", "food", "health", "development", "social", "activity", "mood"
    );
    
    /**
     * Build complete Today page
     */
    public TodayPageResponse buildTodayPage(Long userId, Long profileId, LocalDate date) {
        log.info("Building Today page for user={}, profile={}, date={}", userId, profileId, date);
        
        // 1. Build context
        PersonalizationContext context = contextBuilder.buildContext(userId, profileId, date);
        
        // 2. Calculate scores for all topics
        Map<String, Integer> priorityScores = calculateAllTopicPriorities(context);
        Map<String, Integer> urgencyScores = calculateAllTopicUrgencies(context);
        
        // Store in context
        context.setTopicPriorityScores(priorityScores);
        context.setTopicUrgencyScores(urgencyScores);
        
        // 3. Select top topics
        List<String> topTopics = selectTopTopics(priorityScores, urgencyScores);
        
        // 4. Build sections
        TodayPageResponse.AtAGlanceSection atAGlance = buildAtAGlanceSection(topTopics, context);
        List<TodayPageResponse.AskBabyTopic> askBaby = buildAskBabySection(context);
        List<TodayPageResponse.HeadsUpItem> headsUp = buildHeadsUpSection(context);
        TodayPageResponse.TodaysMomentSection moment = buildMomentSection(context);
        TodayPageResponse.DaytimeCheckinSection checkin = buildCheckinSection(context);
        
        return TodayPageResponse.builder()
            .date(date)
            .profileId(profileId)
            .atAGlance(atAGlance)
            .askBabyAbout(askBaby)
            .headsUp(headsUp)
            .todaysMoment(moment)
            .daytimeCheckin(checkin)
            .build();
    }
    
    private Map<String, Integer> calculateAllTopicPriorities(PersonalizationContext context) {
        Map<String, Integer> scores = new HashMap<>();
        for (String topic : ALL_TOPICS) {
            int score = priorityCalculator.calculateTopicPriority(topic, context);
            scores.put(topic, score);
        }
        return scores;
    }
    
    private Map<String, Integer> calculateAllTopicUrgencies(PersonalizationContext context) {
        Map<String, Integer> scores = new HashMap<>();
        for (String topic : ALL_TOPICS) {
            int score = urgencyCalculator.calculateTopicUrgency(topic, context);
            scores.put(topic, score);
        }
        return scores;
    }
    
    private List<String> selectTopTopics(Map<String, Integer> priorityScores, Map<String, Integer> urgencyScores) {
        // Combine priority and urgency scores
        Map<String, Double> combinedScores = new HashMap<>();
        for (String topic : ALL_TOPICS) {
            int priority = priorityScores.getOrDefault(topic, 0);
            int urgency = urgencyScores.getOrDefault(topic, 0);
            // Weight: 60% priority, 40% urgency
            double combined = (priority * 0.6) + (urgency * 4.0 * 0.4);
            combinedScores.put(topic, combined);
        }
        
        // Sort and select top N
        return combinedScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(maxPillTopics)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private TodayPageResponse.AtAGlanceSection buildAtAGlanceSection(
        List<String> topTopics, PersonalizationContext context) {
        
        List<TodayPageResponse.PillTopic> pills = new ArrayList<>();
        
        for (String topic : topTopics) {
            int priority = context.getTopicPriorityScores().getOrDefault(topic, 0);
            int urgency = context.getTopicUrgencyScores().getOrDefault(topic, 0);
            
            TodayPageResponse.PillTopic pill = TodayPageResponse.PillTopic.builder()
                .title(generatePillTitle(topic, context))
                .topic(topic)
                .priority(mapPriorityLevel(priority))
                .urgency(urgency)
                .summary(generatePillSummary(topic, context))
                .icon(topic)
                .priorityScore(priority)
                .build();
            
            pills.add(pill);
        }
        
        return TodayPageResponse.AtAGlanceSection.builder()
            .pillTopics(pills)
            .summarySentence(generateSummarySentence(context))
            .actionSuggestion(generateActionSuggestion(context))
            .build();
    }
    
    private String generatePillTitle(String topic, PersonalizationContext context) {
        // Simple title generation - can be enhanced with AI
        return switch (topic) {
            case "sleep" -> "Sleep Pattern";
            case "food" -> "Appetite";
            case "health" -> "Health Status";
            case "development" -> "Development";
            case "social" -> "Social";
            case "activity" -> "Activity";
            case "mood" -> "Mood";
            default -> topic;
        };
    }
    
    private String generatePillSummary(String topic, PersonalizationContext context) {
        // Simple summary - can be enhanced with AI
        return "Normal";
    }
    
    private String generateSummarySentence(PersonalizationContext context) {
        if (context.getDailyContext() != null && context.getDailyContext().getHasIncident()) {
            return "Baby had an incident today. Please review details and take appropriate action.";
        }
        return "Baby is doing well today. Continue with current routine.";
    }
    
    private String generateActionSuggestion(PersonalizationContext context) {
        return "Monitor baby's condition and maintain regular schedule.";
    }
    
    private List<TodayPageResponse.AskBabyTopic> buildAskBabySection(PersonalizationContext context) {
        List<TodayPageResponse.AskBabyTopic> suggestions = new ArrayList<>();
        
        // Generate age-appropriate questions
        if (context.getBabyAgeMonths() != null && context.getBabyAgeMonths() >= 18) {
            suggestions.add(TodayPageResponse.AskBabyTopic.builder()
                .topic("food")
                .question("What did you have for lunch today?")
                .context("Age-appropriate conversation")
                .priority("medium")
                .source("age_appropriate")
                .build());
        }
        
        return suggestions;
    }
    
    private List<TodayPageResponse.HeadsUpItem> buildHeadsUpSection(PersonalizationContext context) {
        List<TodayPageResponse.HeadsUpItem> items = new ArrayList<>();
        
        if (context.getActiveReminders() != null) {
            for (PersonalizationContext.ReminderData reminder : context.getActiveReminders()) {
                items.add(TodayPageResponse.HeadsUpItem.builder()
                    .reminderId(reminder.getId())
                    .type(mapReminderType(reminder.getCategory()))
                    .title(reminder.getTitle())
                    .description(reminder.getDescription())
                    .dueDate(reminder.getDueDate())
                    .category(reminder.getCategory())
                    .canSnooze(reminder.getCanSnooze())
                    .canComplete(true)
                    .build());
            }
        }
        
        return items;
    }
    
    private String mapReminderType(String category) {
        return switch (category != null ? category.toLowerCase() : "") {
            case "vaccination" -> "medical";
            case "appointment" -> "medical";
            case "prepare" -> "teacher_note";
            default -> "user_reminder";
        };
    }
    
    private TodayPageResponse.TodaysMomentSection buildMomentSection(PersonalizationContext context) {
        List<Long> highlightIds = new ArrayList<>();
        
        if (context.getTodayMedia() != null && !context.getTodayMedia().isEmpty()) {
            highlightIds = context.getTodayMedia().stream()
                .limit(3)
                .map(PersonalizationContext.MediaData::getId)
                .collect(Collectors.toList());
        }
        
        return TodayPageResponse.TodaysMomentSection.builder()
            .mediaCount(context.getTodayMedia() != null ? context.getTodayMedia().size() : 0)
            .highlightMediaIds(highlightIds)
            .source("user_uploaded")
            .hasNewMedia(!highlightIds.isEmpty())
            .build();
    }
    
    private TodayPageResponse.DaytimeCheckinSection buildCheckinSection(PersonalizationContext context) {
        // Simple checkin question - can be enhanced with question selection service
        return TodayPageResponse.DaytimeCheckinSection.builder()
            .questionId(1L)
            .questionText("How is baby feeling today?")
            .answerType("choice")
            .choices(Arrays.asList("Great", "Good", "Okay", "Not well"))
            .context("Daily wellness check")
            .topic("health")
            .build();
    }
    
    private String mapPriorityLevel(int score) {
        if (score >= 75) return "critical";
        if (score >= 50) return "high";
        if (score >= 25) return "medium";
        return "low";
    }
}
