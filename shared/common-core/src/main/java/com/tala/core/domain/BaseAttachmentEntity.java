package com.tala.core.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Base entity class with attachment support
 * 
 * Extends BaseEntity and implements AttachmentSupport interface.
 * Provides standard JSONB column for storing attachment IDs.
 * 
 * Usage:
 * - Entities needing attachment support should extend this class
 * - Automatically includes id, timestamps, soft delete, and attachment fields
 * - Uses PostgreSQL JSONB for efficient storage and querying
 * 
 * Industry Practice:
 * - JSONB allows flexible array storage without additional tables
 * - Supports GIN indexing for fast queries
 * - Maintains referential integrity through application layer
 * 
 * @author Tala Backend Team
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseAttachmentEntity extends BaseEntity implements AttachmentSupport {
    
    /**
     * Attachment resource IDs stored as JSONB array
     * 
     * Default: empty list (not null)
     * Stores file IDs from file-service or media IDs from media-service
     */
    @Type(JsonBinaryType.class)
    @Column(name = "attachment_ids", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Long> attachmentIds = new ArrayList<>();
    
    @Override
    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }
    
    @Override
    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds != null ? attachmentIds : new ArrayList<>();
    }
}
