package com.tala.origindata.domain;

import com.tala.core.domain.BaseEntity;
import com.tala.origindata.constant.CurriculumItemType;
import com.tala.origindata.constant.CurriculumScope;
import com.tala.origindata.constant.LearningDomain;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.Instant;

/**
 * Weekly Curriculum Detail
 * 
 * Children of weekly_curriculum_header, optimized for timeline/calendar UI.
 * Each row represents a specific curriculum item (theme, topic, skill, event, etc.)
 * that applies either to the whole week or a specific day.
 */
@Entity
@Table(name = "weekly_curriculum_detail", schema = "origin_data", indexes = {
    @Index(name = "idx_weekly_detail_header", columnList = "header_id"),
    @Index(name = "idx_weekly_detail_scope", columnList = "scope"),
    @Index(name = "idx_weekly_detail_day", columnList = "day_of_week"),
    @Index(name = "idx_weekly_detail_type", columnList = "item_type"),
    @Index(name = "idx_weekly_detail_domain", columnList = "learning_domain")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyCurriculumDetail extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = false)
    private WeeklyCurriculumHeader header;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private CurriculumScope scope;
    
    @Column(name = "day_of_week")
    private Integer dayOfWeek;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private CurriculumItemType itemType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "learning_domain", length = 50)
    private LearningDomain learningDomain;
    
    @Column(name = "label", columnDefinition = "TEXT", nullable = false)
    private String label;
    
    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "start_at")
    private Instant startAt;
    
    @Column(name = "end_at")
    private Instant endAt;
    
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Type(JsonBinaryType.class)
    @Column(name = "extra_json", columnDefinition = "jsonb")
    private String extraJson;
}
