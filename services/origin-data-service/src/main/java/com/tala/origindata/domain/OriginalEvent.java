package com.tala.origindata.domain;

import com.tala.core.domain.BaseAttachmentEntity;
import com.tala.core.domain.AttachmentSupport;
import com.tala.origindata.constant.DataSourceType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Original Event - Top-level event sourcing entity
 * 
 * All external data enters through this entity.
 * Provides audit trail, idempotency, and replay capability.
 * 
 * Extends BaseAttachmentEntity for unified attachment support.
 */
@Entity
@Table(name = "original_events", schema = "origin_data", indexes = {
    @Index(name = "idx_original_event_profile_id", columnList = "profile_id"),
    @Index(name = "idx_original_event_source_type", columnList = "source_type"),
    @Index(name = "idx_original_event_source_id", columnList = "source_event_id"),
    @Index(name = "idx_original_event_time", columnList = "event_time"),
    @Index(name = "idx_original_event_ai_processed", columnList = "ai_processed")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OriginalEvent extends BaseAttachmentEntity {
    
    @Column(name = "profile_id", nullable = false)
    private Long profileId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private DataSourceType sourceType;
    
    @Column(name = "source_event_id", length = 255)
    private String sourceEventId;
    
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;
    
    @Type(JsonBinaryType.class)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;
    
    /**
     * Override parent's attachment_ids column to use legacy column name
     * Maintains backward compatibility with existing schema
     */
    @Type(JsonBinaryType.class)
    @Column(name = "attachment_file_ids", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Long> attachmentIds = new ArrayList<>();
    
    @Column(name = "ai_processed")
    @Builder.Default
    private Boolean aiProcessed = false;
    
    @Column(name = "ai_processed_at")
    private Instant aiProcessedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
