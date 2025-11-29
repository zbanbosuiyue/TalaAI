package com.tala.media.service;

import com.tala.media.domain.MediaItem;
import com.tala.media.dto.MediaResponse;
import com.tala.media.repository.MediaItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    
    private final MediaItemRepository repository;
    
    @Transactional(readOnly = true)
    public List<MediaResponse> getMediaByDate(Long profileId, LocalDate date) {
        log.debug("Getting media for profile={}, date={}", profileId, date);
        
        List<MediaItem> items = repository.findByProfileIdAndDate(profileId, date);
        
        return items.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<MediaResponse> getMediaByProfile(Long profileId) {
        log.debug("Getting all media for profile={}", profileId);
        
        List<MediaItem> items = repository.findByProfileIdOrderByOccurredAtDesc(profileId);
        
        return items.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public MediaResponse getMediaById(Long id) {
        MediaItem item = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found: " + id));
        
        return toResponse(item);
    }
    
    @Transactional
    public MediaResponse createMedia(MediaItem item) {
        log.info("Creating media for profile={}", item.getProfileId());
        
        MediaItem saved = repository.save(item);
        return toResponse(saved);
    }
    
    @Transactional
    public void deleteMedia(Long id) {
        log.info("Deleting media id={}", id);
        
        MediaItem item = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found: " + id));
        
        repository.delete(item);
    }
    
    private MediaResponse toResponse(MediaItem item) {
        MediaResponse response = new MediaResponse();
        response.id = item.getId();
        response.profileId = item.getProfileId();
        response.userId = item.getUserId();
        response.source = item.getSource();
        response.mediaType = item.getMediaType();
        response.storageUrl = item.getStorageUrl();
        response.thumbnailUrl = item.getThumbnailUrl();
        response.occurredAt = item.getOccurredAt();
        response.aiTags = item.getAiTags();
        response.facesCount = item.getFacesCount();
        response.emotionScore = item.getEmotionScore();
        return response;
    }
}
