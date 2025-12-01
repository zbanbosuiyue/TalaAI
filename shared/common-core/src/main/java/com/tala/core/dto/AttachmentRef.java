package com.tala.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified attachment reference DTO
 * 
 * Used across all services to represent file/media attachments.
 * Points to file-service or media-service resources.
 * 
 * Industry Practice:
 * - Immutable DTO for API responses
 * - Contains all metadata needed for frontend display
 * - Supports both file-service and media-service
 * - Compatible with multimodal AI processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRef {
    
    /**
     * Source system: FILE_SERVICE, MEDIA_SERVICE, EXTERNAL_URL
     */
    private String source;
    
    /**
     * Resource ID in the source system (fileId or mediaId)
     */
    private Long resourceId;
    
    /**
     * Direct access URL (generated from file-service or media-service)
     */
    private String url;
    
    /**
     * Thumbnail URL (optional, for images/videos)
     */
    private String thumbnailUrl;
    
    /**
     * MIME type (e.g., image/jpeg, video/mp4, application/pdf)
     */
    private String mediaType;
    
    /**
     * Human-readable label (optional)
     */
    private String label;
    
    /**
     * File size in bytes (optional)
     */
    private Long sizeBytes;
    
    /**
     * Image/video width in pixels (optional)
     */
    private Integer width;
    
    /**
     * Image/video height in pixels (optional)
     */
    private Integer height;
    
    /**
     * Video duration in seconds (optional)
     */
    private Integer durationSeconds;
    
    /**
     * Create AttachmentRef from file-service metadata
     */
    public static AttachmentRef fromFileService(Long fileId, String url, String thumbnailUrl, 
                                                 String mimeType, String filename, Long sizeBytes) {
        return AttachmentRef.builder()
                .source("FILE_SERVICE")
                .resourceId(fileId)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .mediaType(mimeType)
                .label(filename)
                .sizeBytes(sizeBytes)
                .build();
    }
    
    /**
     * Create AttachmentRef from media-service metadata
     */
    public static AttachmentRef fromMediaService(Long mediaId, String url, String thumbnailUrl,
                                                  String mediaType, Integer width, Integer height,
                                                  Integer durationSeconds) {
        return AttachmentRef.builder()
                .source("MEDIA_SERVICE")
                .resourceId(mediaId)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .mediaType(mediaType)
                .width(width)
                .height(height)
                .durationSeconds(durationSeconds)
                .build();
    }
}
