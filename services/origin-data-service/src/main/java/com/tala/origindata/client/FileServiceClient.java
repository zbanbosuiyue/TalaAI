package com.tala.origindata.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for File Service
 * Automatically propagates JWT tokens for authenticated requests
 */
@FeignClient(
    name = "file-service", 
    url = "${feign.services.file-service.url:http://localhost:8088}",
    configuration = com.tala.core.feign.FeignJwtConfig.class
)
public interface FileServiceClient {
    
    @GetMapping("/api/v1/files/{fileId}")
    FileMetadataResponse getFileMetadata(@PathVariable("fileId") Long fileId);
    
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
        public Integer width;
        public Integer height;
    }
}
