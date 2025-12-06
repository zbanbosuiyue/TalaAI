package com.tala.origindata.domain;

import com.tala.core.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Weekly Curriculum Header
 * 
 * One row per classroom + week, representing the overall weekly curriculum/newsletter.
 * Stores high-level information like theme, age group, and provider-specific metadata.
 */
@Entity
@Table(name = "weekly_curriculum_header", schema = "origin_data", indexes = {
    @Index(name = "idx_weekly_curriculum_origin_data", columnList = "origin_data_id"),
    @Index(name = "idx_weekly_curriculum_school_classroom", columnList = "school_id,classroom_id"),
    @Index(name = "idx_weekly_curriculum_week_dates", columnList = "week_start_date,week_end_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyCurriculumHeader extends BaseEntity {
    
    @Column(name = "school_id", nullable = false)
    private Long schoolId;
    
    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;
    
    @Column(name = "origin_data_id")
    private Long originDataId;
    
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;
    
    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;
    
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;
    
    @Column(name = "theme", columnDefinition = "TEXT")
    private String theme;
    
    @Column(name = "age_group", columnDefinition = "TEXT")
    private String ageGroup;
    
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;
    
    @Type(JsonBinaryType.class)
    @Column(name = "meta_json", columnDefinition = "jsonb")
    private String metaJson;
    
    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeeklyCurriculumDetail> details = new ArrayList<>();
}
