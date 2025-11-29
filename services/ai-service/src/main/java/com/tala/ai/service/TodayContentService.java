package com.tala.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.ai.dto.TodayOverviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Today At a Glance content generation service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TodayContentService {
    
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    
    /**
     * Generate Today At a Glance content
     */
    public TodayOverviewResponse generateTodayOverview(Long profileId, LocalDate date) {
        log.info("Generating Today overview for profile={}, date={}", profileId, date);
        
        try {
            // Build prompt for Gemini
            String prompt = buildTodayOverviewPrompt(profileId, date);
            
            // Call Gemini API
            String aiResponse = geminiService.generateContent(prompt);
            
            // Parse AI response and build structured response
            return parseTodayOverviewResponse(aiResponse, profileId, date);
            
        } catch (Exception e) {
            log.error("Failed to generate AI content, using fallback", e);
            return generateFallbackOverview(profileId, date);
        }
    }
    
    private String buildTodayOverviewPrompt(Long profileId, LocalDate date) {
        return String.format("""
            You are a caring AI assistant for parents. Generate a brief daily summary for a baby.
            
            Profile ID: %d
            Date: %s
            
            Generate a JSON response with:
            1. summarySentence: A warm 1-2 sentence summary of the baby's day
            2. actionSuggestion: One actionable suggestion for the parent
            3. pillTopics: Array of 3-5 key topics, each with:
               - title: 2-3 words (e.g., "Great Sleep", "Good Appetite")
               - topic: category (sleep/food/health/development/social)
               - priority: low/medium/high
               - description: Brief description
            
            Keep it positive, warm, and actionable.
            Return ONLY valid JSON, no markdown formatting.
            """, profileId, date);
    }
    
    private TodayOverviewResponse parseTodayOverviewResponse(String aiResponse, Long profileId, LocalDate date) {
        try {
            // Parse JSON response from AI
            JsonNode root = objectMapper.readTree(aiResponse);
            
            List<TodayOverviewResponse.PillTopic> pills = new ArrayList<>();
            JsonNode pillsNode = root.path("pillTopics");
            if (pillsNode.isArray()) {
                for (JsonNode pill : pillsNode) {
                    pills.add(TodayOverviewResponse.PillTopic.builder()
                        .title(pill.path("title").asText())
                        .topic(pill.path("topic").asText())
                        .priority(pill.path("priority").asText())
                        .description(pill.path("description").asText())
                        .build());
                }
            }
            
            return TodayOverviewResponse.builder()
                .profileId(profileId)
                .date(date)
                .summarySentence(root.path("summarySentence").asText())
                .actionSuggestion(root.path("actionSuggestion").asText())
                .pillTopics(pills)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            return generateFallbackOverview(profileId, date);
        }
    }
    
    private TodayOverviewResponse generateFallbackOverview(Long profileId, LocalDate date) {
        List<TodayOverviewResponse.PillTopic> pills = new ArrayList<>();
        pills.add(TodayOverviewResponse.PillTopic.builder()
            .title("Normal Day")
            .topic("general")
            .priority("medium")
            .description("Baby is doing well")
            .build());
        
        return TodayOverviewResponse.builder()
            .profileId(profileId)
            .date(date)
            .summarySentence("Baby is doing well today.")
            .actionSuggestion("Continue with the current routine.")
            .pillTopics(pills)
            .build();
    }
    
    /**
     * Generate Ask Baby About suggestions
     */
    public List<String> generateAskBabySuggestions(Long profileId, LocalDate date) {
        log.info("Generating Ask Baby suggestions for profile={}, date={}", profileId, date);
        
        try {
            String prompt = String.format("""
                Generate 3-5 age-appropriate conversation starters for a parent to ask their toddler.
                
                Profile ID: %d
                Date: %s
                
                Topics to cover: food, friends, activities, feelings, learning
                
                Return as JSON array of strings, e.g.:
                ["What did you have for lunch?", "Who did you play with?", "What was your favorite activity?"]
                
                Keep questions simple, warm, and engaging for young children.
                Return ONLY valid JSON array, no markdown.
                """, profileId, date);
            
            String aiResponse = geminiService.generateContent(prompt);
            
            // Parse JSON array
            JsonNode root = objectMapper.readTree(aiResponse);
            List<String> suggestions = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    suggestions.add(item.asText());
                }
            }
            
            return suggestions.isEmpty() ? generateFallbackSuggestions() : suggestions;
            
        } catch (Exception e) {
            log.error("Failed to generate Ask Baby suggestions", e);
            return generateFallbackSuggestions();
        }
    }
    
    private List<String> generateFallbackSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("What did you do today?");
        suggestions.add("Who did you play with?");
        suggestions.add("What was your favorite part of the day?");
        return suggestions;
    }
}
