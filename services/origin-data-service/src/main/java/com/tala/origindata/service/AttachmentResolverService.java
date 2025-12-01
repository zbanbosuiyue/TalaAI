package com.tala.origindata.service;

import com.tala.core.domain.AttachmentSupport;
import com.tala.core.dto.AttachmentRef;
import com.tala.core.service.AttachmentResolver;
import com.tala.origindata.client.FileServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves attachment file IDs to AttachmentRef DTOs
 * 
 * Implements unified AttachmentResolver interface.
 * Calls file-service to get metadata and constructs unified attachment references.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentResolverService implements AttachmentResolver {
    
    private final FileServiceClient fileServiceClient;
    
    @Override
    public List<AttachmentRef> resolve(List<Long> attachmentIds, AttachmentSupport.AttachmentSourceType sourceType) {
        if (!supports(sourceType)) {
            log.warn("Unsupported attachment source type: {}", sourceType);
            return List.of();
        }
        return resolveAttachments(attachmentIds);
    }
    
    @Override
    public boolean supports(AttachmentSupport.AttachmentSourceType sourceType) {
        return sourceType == AttachmentSupport.AttachmentSourceType.FILE_SERVICE;
    }
    
    /**
     * Resolve file IDs to attachment references
     * 
     * @param fileIds List of file-service file IDs
     * @return List of AttachmentRef with URLs and metadata
     */
    public List<AttachmentRef> resolveAttachments(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        
        List<AttachmentRef> attachments = new ArrayList<>();
        
        for (Long fileId : fileIds) {
            try {
                FileServiceClient.FileMetadataResponse metadata = fileServiceClient.getFileMetadata(fileId);
                
                AttachmentRef ref = AttachmentRef.fromFileService(
                    metadata.id,
                    metadata.publicUrl,
                    metadata.thumbnailUrl,
                    metadata.mimeType,
                    metadata.originalFilename,
                    metadata.fileSize
                );
                
                attachments.add(ref);
                
            } catch (Exception e) {
                log.warn("Failed to resolve file ID {}: {}", fileId, e.getMessage());
                // Continue with other files, don't fail entire request
            }
        }
        
        return attachments;
    }
}
