package com.tala.origindata.domain;

import com.tala.core.domain.BaseAttachmentEntity;
import com.tala.origindata.constant.DataSourceType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;

/**
 * Origin Data - Top-level data sourcing entity
 * 
 * All external data enters through this entity.
 * Supports both event-based data (feeding, sleep, etc.) and document-based data (curriculum, reports).
 * Provides audit trail, idempotency, and replay capability.
 * 
 * Extends BaseAttachmentEntity for unified attachment support.
 */
@Entity
@Table(name = "origin_data", schema = "origin_data", indexes = {
    @Index(name = "idx_origin_data_profile_id", columnList = "profile_id"),
    @Index(name = "idx_origin_data_source_type", columnList = "source_type"),
    @Index(name = "idx_origin_data_source_id", columnList = "source_data_id"),
    @Index(name = "idx_origin_data_time", columnList = "data_time"),
    @Index(name = "idx_origin_data_ai_processed", columnList = "ai_processed")
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
    
    @Column(name = "source_data_id", length = 255)
    private String sourceEventId;
    
    @Column(name = "data_time", nullable = false)
    private Instant eventTime;
    
    @Type(JsonBinaryType.class)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;
    
    @Column(name = "ai_processed")
    @Builder.Default
    private Boolean aiProcessed = false;
    
    @Column(name = "ai_processed_at")
    private Instant aiProcessedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
