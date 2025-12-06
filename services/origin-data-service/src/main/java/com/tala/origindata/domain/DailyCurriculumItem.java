package com.tala.origindata.domain;

import com.tala.core.domain.BaseEntity;
import com.tala.origindata.constant.LearningDomain;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

/**
 * Daily Curriculum Item (Detail)
 * 
 * Domain-level planned activities for a specific day.
 * Each row represents an activity within a particular learning domain
 * (e.g., Cognitive, Language, Physical, etc.)
 */
@Entity
@Table(name = "daily_curriculum_item", schema = "origin_data", indexes = {
    @Index(name = "idx_daily_item_day", columnList = "day_id"),
    @Index(name = "idx_daily_item_domain", columnList = "domain")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCurriculumItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private DailyCurriculumDay day;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 50)
    private LearningDomain domain;
    
    @Column(name = "activity", columnDefinition = "TEXT", nullable = false)
    private String activity;
    
    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;
    
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Type(JsonBinaryType.class)
    @Column(name = "extra_json", columnDefinition = "jsonb")
    private String extraJson;
}
