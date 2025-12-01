package com.tala.core.service;

import com.tala.core.domain.AttachmentSupport;
import com.tala.core.dto.AttachmentRef;

import java.util.List;

/**
 * Attachment Resolver Interface
 * 
 * Defines contract for resolving attachment IDs to AttachmentRef DTOs.
 * Each service can implement this interface to provide attachment resolution.
 * 
 * Industry Practice:
 * - Strategy pattern for different attachment sources
 * - Allows polymorphic attachment handling
 * - Supports dependency injection and testing
 * 
 * @author Tala Backend Team
 */
public interface AttachmentResolver {
    
    /**
     * Resolve attachment IDs to AttachmentRef DTOs
     * 
     * @param attachmentIds List of resource IDs (file IDs or media IDs)
     * @param sourceType Source type of the attachments
     * @return List of resolved AttachmentRef with URLs and metadata
     */
    List<AttachmentRef> resolve(List<Long> attachmentIds, AttachmentSupport.AttachmentSourceType sourceType);
    
    /**
     * Resolve attachments from an entity
     * 
     * @param entity Entity implementing AttachmentSupport
     * @return List of resolved AttachmentRef
     */
    default List<AttachmentRef> resolve(AttachmentSupport entity) {
        if (entity == null || !entity.hasAttachments()) {
            return List.of();
        }
        return resolve(entity.getAttachmentIds(), entity.getAttachmentSourceType());
    }
    
    /**
     * Check if resolver supports the given source type
     * 
     * @param sourceType Source type to check
     * @return true if this resolver can handle the source type
     */
    boolean supports(AttachmentSupport.AttachmentSourceType sourceType);
}
