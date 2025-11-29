package com.tala.media.repository;

import com.tala.media.domain.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    
    List<MediaItem> findByProfileIdAndOccurredAtBetween(Long profileId, Instant startDate, Instant endDate);
    
    List<MediaItem> findByProfileIdOrderByOccurredAtDesc(Long profileId);
    
    @Query("SELECT m FROM MediaItem m WHERE m.profileId = :profileId AND CAST(m.occurredAt AS date) = :date AND m.deletedAt IS NULL")
    List<MediaItem> findByProfileIdAndDate(Long profileId, LocalDate date);
    
    List<MediaItem> findByProfileIdAndSourceOrderByOccurredAtDesc(Long profileId, String source);
    
    @Query("SELECT COUNT(m) FROM MediaItem m WHERE m.profileId = :profileId AND m.deletedAt IS NULL")
    Long countByProfileId(Long profileId);
}
