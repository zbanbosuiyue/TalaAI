package com.tala.file.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * File Security Validator
 * 
 * Industry best practices for file upload security:
 * - File size limits
 * - MIME type validation
 * - File extension whitelist
 * - Content type verification
 * - Malicious file detection
 */
@Component
@Slf4j
public class FileSecurityValidator {
    
    @Value("${file.upload.max-size:104857600}") // 100MB default
    private long maxFileSize;
    
    @Value("${file.upload.max-image-size:10485760}") // 10MB default for images
    private long maxImageSize;
    
    // Allowed MIME types
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/heic",
        "image/heif"
    );
    
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
        "video/mp4",
        "video/quicktime",
        "video/x-msvideo",
        "video/webm"
    );
    
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    // Allowed file extensions
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "heic", "heif"
    );
    
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
        "mp4", "mov", "avi", "webm"
    );
    
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "txt"
    );
    
    // Dangerous file extensions to block
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "app", "deb", "rpm"
    );
    
    /**
     * Validate file upload
     */
    public ValidationResult validate(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("File is empty");
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            return ValidationResult.error(
                String.format("File size exceeds maximum allowed size of %d MB", maxFileSize / 1024 / 1024)
            );
        }
        
        // Get file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ValidationResult.error("Invalid filename");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        // Check for blocked extensions
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            log.warn("Blocked file upload attempt: userId={}, filename={}, extension={}", 
                    userId, originalFilename, extension);
            return ValidationResult.error("File type not allowed");
        }
        
        // Get content type
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            return ValidationResult.error("Invalid content type");
        }
        
        // Validate based on content type
        if (contentType.startsWith("image/")) {
            return validateImage(file, extension, contentType);
        } else if (contentType.startsWith("video/")) {
            return validateVideo(file, extension, contentType);
        } else if (isDocumentType(contentType)) {
            return validateDocument(file, extension, contentType);
        } else {
            return ValidationResult.error("Unsupported file type: " + contentType);
        }
    }
    
    /**
     * Validate image file
     */
    private ValidationResult validateImage(MultipartFile file, String extension, String contentType) {
        // Check size
        if (file.getSize() > maxImageSize) {
            return ValidationResult.error(
                String.format("Image size exceeds maximum allowed size of %d MB", maxImageSize / 1024 / 1024)
            );
        }
        
        // Check extension
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return ValidationResult.error("Image file extension not allowed: " + extension);
        }
        
        // Check MIME type
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return ValidationResult.error("Image MIME type not allowed: " + contentType);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate video file
     */
    private ValidationResult validateVideo(MultipartFile file, String extension, String contentType) {
        // Check extension
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            return ValidationResult.error("Video file extension not allowed: " + extension);
        }
        
        // Check MIME type
        if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return ValidationResult.error("Video MIME type not allowed: " + contentType);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate document file
     */
    private ValidationResult validateDocument(MultipartFile file, String extension, String contentType) {
        // Check extension
        if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            return ValidationResult.error("Document file extension not allowed: " + extension);
        }
        
        // Check MIME type
        if (!ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            return ValidationResult.error("Document MIME type not allowed: " + contentType);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Check if content type is a document
     */
    private boolean isDocumentType(String contentType) {
        return contentType.startsWith("application/") || contentType.startsWith("text/");
    }
    
    /**
     * Extract file extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
