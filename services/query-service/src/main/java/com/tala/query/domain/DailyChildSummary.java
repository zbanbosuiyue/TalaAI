package com.tala.query.domain;

import com.tala.core.domain.BaseEntity;
import com.tala.core.domain.AttachmentSupport;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Daily child summary for aggregated metrics
 * 
 * Implements AttachmentSupport for media attachments.
 * Uses MEDIA_SERVICE source type for photo/video highlights.
 */
@Entity
@Table(
    name = "daily_child_summaries",
    schema = "analytics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_profile_date", columnNames = {"profile_id", "date"})
    },
    indexes = {
        @Index(name = "idx_profile_date", columnList = "profile_id, date")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChildSummary extends BaseEntity implements AttachmentSupport {
    
    @Column(name = "profile_id", nullable = false)
    private Long profileId;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Type(JsonBinaryType.class)
    @Column(name = "events_summary", columnDefinition = "jsonb")
    private Map<String, Object> eventsSummary;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;
    
    /**
     * Media attachment IDs (photos/videos from media-service)
     * Implements AttachmentSupport interface
     */
    @Type(JsonBinaryType.class)
    @Column(name = "candidate_media_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<Long> attachmentIds = new ArrayList<>();
    
    @Type(JsonBinaryType.class)
    @Column(name = "candidate_incident_ids", columnDefinition = "jsonb")
    private List<Long> candidateIncidentIds;
    
    @Column(name = "total_events")
    private Integer totalEvents;
    
    @Column(name = "has_incident")
    private Boolean hasIncident;
    
    @Column(name = "has_sickness")
    private Boolean hasSickness;
    
    @Override
    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }
    
    @Override
    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds != null ? attachmentIds : new ArrayList<>();
    }
    
    @Override
    public AttachmentSourceType getAttachmentSourceType() {
        return AttachmentSourceType.MEDIA_SERVICE;
    }
}
