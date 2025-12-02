package com.tala.file.controller;

import com.tala.file.domain.FileMetadata;
import com.tala.file.security.FileSecurityValidator;
import com.tala.file.service.FileManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * File upload and management API
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    
    private final FileManagementService fileManagementService;
    private final FileSecurityValidator securityValidator;
    
    /**
     * Upload file with security validation
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("userId") Long userId,
        @RequestParam(value = "profileId", required = false) Long profileId
    ) {
        log.info("POST /api/v1/files/upload - userId={}, filename={}", userId, file.getOriginalFilename());
        
        // Validate file security
        FileSecurityValidator.ValidationResult validationResult = securityValidator.validate(file, userId);
        if (!validationResult.isValid()) {
            log.warn("File validation failed: userId={}, filename={}, error={}", 
                    userId, file.getOriginalFilename(), validationResult.getErrorMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", validationResult.getErrorMessage()));
        }
        
        try {
            FileMetadata metadata = fileManagementService.uploadFile(file, userId, profileId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
    
    /**
     * Get file metadata
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable Long fileId) {
        log.debug("GET /api/v1/files/{}", fileId);
        return fileManagementService.getFileMetadata(fileId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Download file
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long fileId) {
        log.debug("GET /api/v1/files/{}/download", fileId);
        
        try {
            FileMetadata metadata = fileManagementService.getFileMetadata(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
            
            InputStream stream = fileManagementService.getFileStream(metadata.getStorageKey());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                .body(new InputStreamResource(stream));
        } catch (Exception e) {
            log.error("Failed to download file", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user files
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FileMetadata>> getUserFiles(@PathVariable Long userId) {
        log.debug("GET /api/v1/files/user/{}", userId);
        List<FileMetadata> files = fileManagementService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }
    
    /**
     * Get profile files
     */
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<List<FileMetadata>> getProfileFiles(@PathVariable Long profileId) {
        log.debug("GET /api/v1/files/profile/{}", profileId);
        List<FileMetadata> files = fileManagementService.getProfileFiles(profileId);
        return ResponseEntity.ok(files);
    }
    
    /**
     * Delete file
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        log.info("DELETE /api/v1/files/{}", fileId);
        
        try {
            fileManagementService.deleteFile(fileId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete file", e);
            return ResponseEntity.notFound().build();
        }
    }
}
