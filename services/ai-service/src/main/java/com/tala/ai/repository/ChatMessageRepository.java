package com.tala.ai.repository;

import com.tala.ai.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Chat Message Repository
 * 
 * @author Tala Team
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find message by ID (not deleted)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<ChatMessage> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find messages by profile ID (paginated, not deleted)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.deletedAt IS NULL ORDER BY m.createdAt ASC")
    Page<ChatMessage> findByProfileIdAndNotDeleted(@Param("profileId") Long profileId, Pageable pageable);

    /**
     * Find all messages by profile ID (not deleted)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.deletedAt IS NULL ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllByProfileIdAndNotDeleted(@Param("profileId") Long profileId);

    /**
     * Find recent messages by profile ID (for context)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesByProfileId(@Param("profileId") Long profileId, Pageable pageable);

    /**
     * Count messages by profile ID (not deleted)
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.profileId = :profileId AND m.deletedAt IS NULL")
    long countByProfileIdAndNotDeleted(@Param("profileId") Long profileId);

    /**
     * Find messages by profile ID and user ID (not deleted)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.userId = :userId AND m.deletedAt IS NULL ORDER BY m.createdAt ASC")
    List<ChatMessage> findByProfileIdAndUserIdAndNotDeleted(@Param("profileId") Long profileId, @Param("userId") Long userId);

    /**
     * Find messages newer than specified ID (for loading new messages)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.id > :messageId AND m.deletedAt IS NULL ORDER BY m.createdAt ASC")
    Page<ChatMessage> findByProfileIdAndIdGreaterThanAndNotDeleted(
            @Param("profileId") Long profileId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );

    /**
     * Find messages older than specified ID (for loading history)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.profileId = :profileId AND m.id < :messageId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByProfileIdAndIdLessThanAndNotDeleted(
            @Param("profileId") Long profileId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );
}
