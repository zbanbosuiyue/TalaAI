package com.tala.ai.service;

import com.tala.ai.domain.AiModelUsage;
import com.tala.ai.repository.AiModelUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Model Usage Service
 * 
 * Responsibilities:
 * - Record every AI model invocation details
 * - Provide token usage statistics
 * - Support cost analysis and optimization decisions
 * 
 * @author Tala Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiModelUsageService {
    
    private final AiModelUsageRepository repository;
    
    /**
     * Record AI model usage
     */
    @Transactional
    public void recordUsage(AiModelUsage usage) {
        try {
            // Calculate total tokens if not set
            if (usage.getTotalTokens() == null && usage.getInputTokens() != null && usage.getOutputTokens() != null) {
                usage.setTotalTokens(usage.getInputTokens() + usage.getOutputTokens());
            }
            
            repository.save(usage);
            log.debug("✅ Recorded AI model usage: stage={}, tokens={}, success={}", 
                    usage.getStageName(), usage.getTotalTokens(), usage.getIsSuccess());
        } catch (Exception e) {
            // Recording failure should not affect main flow, only log error
            log.error("❌ Failed to record AI model usage: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get user usage history within time range
     */
    public List<AiModelUsage> getUserUsageHistory(Long userId, LocalDateTime start, LocalDateTime end) {
        return repository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }
    
    /**
     * Get profile usage history within time range
     */
    public List<AiModelUsage> getProfileUsageHistory(Long profileId, LocalDateTime start, LocalDateTime end) {
        return repository.findByProfileIdAndCreatedAtBetween(profileId, start, end);
    }
    
    /**
     * Sum total tokens by user ID and period
     */
    public Long getUserTotalTokens(Long userId, LocalDateTime start, LocalDateTime end) {
        return repository.sumTokensByUserIdAndPeriod(userId, start, end);
    }
    
    /**
     * Sum total tokens by profile ID and period
     */
    public Long getProfileTotalTokens(Long profileId, LocalDateTime start, LocalDateTime end) {
        return repository.sumTokensByProfileIdAndPeriod(profileId, start, end);
    }
}
