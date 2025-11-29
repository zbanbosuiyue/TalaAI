package com.tala.personalization.service;

import com.tala.personalization.client.*;
import com.tala.personalization.dto.PersonalizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Context builder - aggregates data from multiple services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextBuilder {
    
    private final QueryServiceClient queryServiceClient;
    private final ReminderServiceClient reminderServiceClient;
    private final MediaServiceClient mediaServiceClient;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    
    /**
     * Build complete personalization context
     */
    public PersonalizationContext buildContext(Long userId, Long profileId, LocalDate date) {
        log.info("Building context for user={}, profile={}, date={}", userId, profileId, date);
        
        try {
            // Parallel API calls for performance
            CompletableFuture<PersonalizationContext.DailyContextData> dailyContextFuture = 
                CompletableFuture.supplyAsync(() -> fetchDailyContext(profileId, date));
            
            CompletableFuture<List<PersonalizationContext.ReminderData>> remindersFuture = 
                CompletableFuture.supplyAsync(() -> fetchReminders(userId, profileId, date));
            
            CompletableFuture<List<PersonalizationContext.MediaData>> mediaFuture = 
                CompletableFuture.supplyAsync(() -> fetchMedia(profileId, date));
            
            CompletableFuture<PersonalizationContext.InterestProfileData> interestsFuture = 
                CompletableFuture.supplyAsync(() -> fetchInterestProfile(userId, profileId));
            
            CompletableFuture<UserServiceClient.ProfileResponse> profileFuture = 
                CompletableFuture.supplyAsync(() -> fetchProfile(profileId));
            
            CompletableFuture<List<PersonalizationContext.RecentEventData>> eventsFuture = 
                CompletableFuture.supplyAsync(() -> fetchRecentEvents(profileId, date));
            
            // Wait for all futures to complete
            CompletableFuture.allOf(
                dailyContextFuture, remindersFuture, mediaFuture, 
                interestsFuture, profileFuture, eventsFuture
            ).join();
            
            // Get results
            PersonalizationContext.DailyContextData dailyContext = dailyContextFuture.get();
            List<PersonalizationContext.ReminderData> reminders = remindersFuture.get();
            List<PersonalizationContext.MediaData> media = mediaFuture.get();
            PersonalizationContext.InterestProfileData interests = interestsFuture.get();
            UserServiceClient.ProfileResponse profile = profileFuture.get();
            List<PersonalizationContext.RecentEventData> events = eventsFuture.get();
            
            // Build context
            return PersonalizationContext.builder()
                .userId(userId)
                .profileId(profileId)
                .date(date)
                .babyAgeMonths(profile.ageMonths)
                .babyName(profile.childName)
                .dailyContext(dailyContext)
                .activeReminders(reminders)
                .todayMedia(media)
                .interestProfile(interests)
                .recentEvents(events)
                .topicPriorityScores(new HashMap<>())
                .topicUrgencyScores(new HashMap<>())
                .topicTrends(new HashMap<>())
                .build();
                
        } catch (Exception e) {
            log.error("Error building context", e);
            // Return minimal context on error
            return PersonalizationContext.builder()
                .userId(userId)
                .profileId(profileId)
                .date(date)
                .topicPriorityScores(new HashMap<>())
                .topicUrgencyScores(new HashMap<>())
                .build();
        }
    }
    
    private PersonalizationContext.DailyContextData fetchDailyContext(Long profileId, LocalDate date) {
        try {
            QueryServiceClient.DailyContextResponse response = 
                queryServiceClient.getDailyContext(profileId, date);
            
            List<PersonalizationContext.TrendData> trends = response.recentTrends != null ?
                response.recentTrends.stream()
                    .map(t -> PersonalizationContext.TrendData.builder()
                        .metric(t.metric)
                        .trend(t.trend)
                        .changePercent(t.changePercent)
                        .description(t.description)
                        .build())
                    .collect(Collectors.toList()) : new ArrayList<>();
            
            return PersonalizationContext.DailyContextData.builder()
                .totalEvents(response.totalEvents)
                .hasIncident(response.hasIncident)
                .hasSickness(response.hasSickness)
                .eventsSummary(response.eventsSummary)
                .metrics(response.metrics)
                .recentTrends(trends)
                .build();
        } catch (Exception e) {
            log.warn("Failed to fetch daily context: {}", e.getMessage());
            return PersonalizationContext.DailyContextData.builder()
                .totalEvents(0)
                .hasIncident(false)
                .hasSickness(false)
                .build();
        }
    }
    
    private List<PersonalizationContext.ReminderData> fetchReminders(Long userId, Long profileId, LocalDate date) {
        try {
            List<ReminderServiceClient.ReminderResponse> reminders = 
                reminderServiceClient.getDueReminders(userId, profileId, date);
            
            return reminders.stream()
                .map(r -> PersonalizationContext.ReminderData.builder()
                    .id(r.id)
                    .title(r.title)
                    .description(r.description)
                    .category(r.category)
                    .dueDate(r.dueAt)
                    .priority(r.priority)
                    .canSnooze(r.canSnooze)
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch reminders: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<PersonalizationContext.MediaData> fetchMedia(Long profileId, LocalDate date) {
        try {
            List<MediaServiceClient.MediaResponse> media = 
                mediaServiceClient.getMediaByDate(profileId, date);
            
            return media.stream()
                .map(m -> PersonalizationContext.MediaData.builder()
                    .id(m.id)
                    .source(m.source)
                    .mediaType(m.mediaType)
                    .aiTags(m.aiTags)
                    .emotionScore(m.emotionScore)
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch media: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private PersonalizationContext.InterestProfileData fetchInterestProfile(Long userId, Long profileId) {
        try {
            UserServiceClient.InterestScoresResponse response = 
                userServiceClient.getInterestScores(userId, profileId);
            
            return PersonalizationContext.InterestProfileData.builder()
                .interestVector(response.interestVector)
                .explicitTopics(response.explicitTopics)
                .recentTopics(response.recentTopics)
                .build();
        } catch (Exception e) {
            log.warn("Failed to fetch interest profile: {}", e.getMessage());
            return PersonalizationContext.InterestProfileData.builder()
                .interestVector(new HashMap<>())
                .explicitTopics(new ArrayList<>())
                .recentTopics(new ArrayList<>())
                .build();
        }
    }
    
    private UserServiceClient.ProfileResponse fetchProfile(Long profileId) {
        try {
            return userServiceClient.getProfile(profileId);
        } catch (Exception e) {
            log.error("Failed to fetch profile: {}", e.getMessage());
            UserServiceClient.ProfileResponse fallback = new UserServiceClient.ProfileResponse();
            fallback.id = profileId;
            fallback.childName = "Baby";
            fallback.ageMonths = 12;
            return fallback;
        }
    }
    
    private List<PersonalizationContext.RecentEventData> fetchRecentEvents(Long profileId, LocalDate date) {
        try {
            Instant endTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant startTime = date.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            List<EventServiceClient.EventResponse> events = 
                eventServiceClient.getTimeline(profileId, startTime, endTime);
            
            return events.stream()
                .map(e -> PersonalizationContext.RecentEventData.builder()
                    .id(e.id)
                    .eventType(e.eventType)
                    .priority(e.priority)
                    .urgencyHours(e.urgencyHours)
                    .riskLevel(e.riskLevel)
                    .occurredAt(e.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate())
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch recent events: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
