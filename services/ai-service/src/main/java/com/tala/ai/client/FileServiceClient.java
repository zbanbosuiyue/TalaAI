package com.tala.ai.client;

import com.tala.core.feign.FeignJwtConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for File Service
 * 
 * Automatically propagates JWT tokens from incoming requests to file-service.
 * Uses FeignJwtConfig for JWT token propagation.
 */
@FeignClient(
    name = "file-service",
    url = "${services.file-service.url}",
    configuration = FeignJwtConfig.class
)
public interface FileServiceClient {
    
    /**
     * Get file metadata by ID
     */
    @GetMapping("/api/v1/files/{fileId}")
    FileMetadataResponse getFileMetadata(@PathVariable("fileId") Long fileId);
    
    /**
     * File metadata response from file-service
     */
    class FileMetadataResponse {
        public Long id;
        public Long userId;
        public Long profileId;
        public String originalFilename;
        public String storageKey;
        public String fileType;
        public String mimeType;
        public Long fileSize;
        public String publicUrl;
        public String thumbnailUrl;
        public String checksum;
        public Integer width;
        public Integer height;
    }
}
