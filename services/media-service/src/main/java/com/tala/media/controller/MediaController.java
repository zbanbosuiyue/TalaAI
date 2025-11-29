package com.tala.media.controller;

import com.tala.media.dto.MediaResponse;
import com.tala.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {
    
    private final MediaService mediaService;
    
    @GetMapping
    public ResponseEntity<List<MediaResponse>> getMedia(
        @RequestParam Long profileId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/v1/media - profileId={}, date={}", profileId, date);
        
        List<MediaResponse> response = date != null ?
            mediaService.getMediaByDate(profileId, date) :
            mediaService.getMediaByProfile(profileId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getMediaById(@PathVariable Long id) {
        log.info("GET /api/v1/media/{}", id);
        
        MediaResponse response = mediaService.getMediaById(id);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id) {
        log.info("DELETE /api/v1/media/{}", id);
        
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
