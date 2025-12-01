package com.tala.ai.repository;

import com.tala.ai.domain.AiModelUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Model Usage Repository
 * 
 * @author Tala Team
 */
@Repository
public interface AiModelUsageRepository extends JpaRepository<AiModelUsage, Long> {
    
    /**
     * Find AI usage records by user ID and time range
     */
    List<AiModelUsage> findByUserIdAndCreatedAtBetween(
            Long userId, 
            LocalDateTime start, 
            LocalDateTime end);
    
    /**
     * Find AI usage records by profile ID and time range
     */
    List<AiModelUsage> findByProfileIdAndCreatedAtBetween(
            Long profileId, 
            LocalDateTime start, 
            LocalDateTime end);
    
    /**
     * Sum total tokens by user ID and period
     */
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AiModelUsage u " +
           "WHERE u.userId = :userId " +
           "AND u.createdAt BETWEEN :start AND :end " +
           "AND u.isSuccess = true")
    Long sumTokensByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    /**
     * Sum total tokens by profile ID and period
     */
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AiModelUsage u " +
           "WHERE u.profileId = :profileId " +
           "AND u.createdAt BETWEEN :start AND :end " +
           "AND u.isSuccess = true")
    Long sumTokensByProfileIdAndPeriod(
            @Param("profileId") Long profileId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
