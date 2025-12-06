package com.tala.file.service;

import com.tala.file.domain.FileMetadata;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * File storage service using MinIO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.public-url}")
    private String publicUrl;
    
    @Value("${file.storage.allowed-extensions}")
    private String allowedExtensions;
    
    /**
     * Upload file to MinIO
     */
    public FileMetadata uploadFile(MultipartFile file, Long userId, Long profileId) throws Exception {
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String storageKey = generateStorageKey(userId, extension);
        
        // Calculate checksum
        String checksum = calculateChecksum(file.getInputStream());
        
        // Upload main file
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(storageKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        String fileUrl = String.format("%s/%s/%s", publicUrl, bucketName, storageKey);
        
        // Build metadata
        FileMetadata metadata = FileMetadata.builder()
            .userId(userId)
            .profileId(profileId)
            .originalFilename(originalFilename)
            .storageKey(storageKey)
            .fileType(determineFileType(file.getContentType()))
            .mimeType(file.getContentType())
            .fileSize(file.getSize())
            .storagePath(storageKey)
            .publicUrl(fileUrl)
            .checksum(checksum)
            .build();
        
        // Generate thumbnail for images
        if (isImage(file.getContentType())) {
            try {
                String thumbnailUrl = generateThumbnail(file, storageKey);
                metadata.setThumbnailUrl(thumbnailUrl);
                
                // Get image dimensions
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null) {
                    metadata.setWidth(image.getWidth());
                    metadata.setHeight(image.getHeight());
                }
            } catch (Exception e) {
                log.warn("Failed to generate thumbnail for {}: {}", storageKey, e.getMessage());
            }
        }
        
        return metadata;
    }
    
    /**
     * Generate thumbnail for image
     */
    private String generateThumbnail(MultipartFile file, String storageKey) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(file.getInputStream())
            .size(300, 300)
            .outputFormat("jpg")
            .toOutputStream(outputStream);
        
        byte[] thumbnailBytes = outputStream.toByteArray();
        String thumbnailKey = "thumbnails/" + storageKey;
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(thumbnailKey)
                .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                .contentType("image/jpeg")
                .build()
        );
        
        return String.format("%s/%s/%s", publicUrl, bucketName, thumbnailKey);
    }
    
    /**
     * Delete file from MinIO
     */
    public void deleteFile(String storageKey) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(storageKey)
                .build()
        );
        
        // Try to delete thumbnail
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object("thumbnails/" + storageKey)
                    .build()
            );
        } catch (Exception e) {
            log.debug("No thumbnail to delete for {}", storageKey);
        }
    }
    
    /**
     * Get file stream
     */
    public InputStream getFileStream(String storageKey) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(storageKey)
                .build()
        );
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }
    }
    
    private String generateStorageKey(Long userId, String extension) {
        // Industry best practice: Use UUID-only for storage key
        // This prevents exposing user information in URLs and simplifies access control
        return String.format("%s_%s.%s", UUID.randomUUID(), System.currentTimeMillis(), extension);
    }
    
    private String determineFileType(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.equals("application/pdf")) return "document";
        return "other";
    }
    
    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
    
    private String calculateChecksum(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
