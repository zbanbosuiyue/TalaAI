package com.tala.core.domain;

import java.util.List;

/**
 * Interface for entities that support attachments
 * 
 * Provides a unified contract for attachment management across all services.
 * Entities implementing this interface can store references to files/media.
 * 
 * Industry Practice:
 * - Similar to Spring Data's Auditable interface
 * - Enables polymorphic attachment handling
 * - Supports both file-service and media-service resources
 * 
 * @author Tala Backend Team
 */
public interface AttachmentSupport {
    
    /**
     * Get attachment resource IDs
     * 
     * @return List of resource IDs (file IDs or media IDs)
     */
    List<Long> getAttachmentIds();
    
    /**
     * Set attachment resource IDs
     * 
     * @param attachmentIds List of resource IDs to store
     */
    void setAttachmentIds(List<Long> attachmentIds);
    
    /**
     * Get attachment source type
     * 
     * @return Source type: FILE_SERVICE, MEDIA_SERVICE, or MIXED
     */
    default AttachmentSourceType getAttachmentSourceType() {
        return AttachmentSourceType.FILE_SERVICE;
    }
    
    /**
     * Check if entity has attachments
     * 
     * @return true if attachments exist
     */
    default boolean hasAttachments() {
        List<Long> ids = getAttachmentIds();
        return ids != null && !ids.isEmpty();
    }
    
    /**
     * Get attachment count
     * 
     * @return Number of attachments
     */
    default int getAttachmentCount() {
        List<Long> ids = getAttachmentIds();
        return ids != null ? ids.size() : 0;
    }
    
    /**
     * Attachment source type enum
     */
    enum AttachmentSourceType {
        /** Files from file-service (documents, PDFs, etc.) */
        FILE_SERVICE,
        
        /** Media from media-service (photos, videos) */
        MEDIA_SERVICE,
        
        /** Mixed sources (both file-service and media-service) */
        MIXED
    }
}
