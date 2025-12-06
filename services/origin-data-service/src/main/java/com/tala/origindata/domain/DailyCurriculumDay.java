package com.tala.origindata.domain;

import com.tala.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily Curriculum Day (Header)
 * 
 * One row per classroom + date describing the planned activities by development domain.
 * Links to the parent weekly curriculum if available.
 */
@Entity
@Table(name = "daily_curriculum_day", schema = "origin_data", indexes = {
    @Index(name = "idx_daily_curriculum_origin_data", columnList = "origin_data_id"),
    @Index(name = "idx_daily_curriculum_school_classroom", columnList = "school_id,classroom_id"),
    @Index(name = "idx_daily_curriculum_date", columnList = "date"),
    @Index(name = "idx_daily_curriculum_week_header", columnList = "week_header_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCurriculumDay extends BaseEntity {
    
    @Column(name = "school_id", nullable = false)
    private Long schoolId;
    
    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;
    
    @Column(name = "origin_data_id")
    private Long originDataId;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "week_header_id")
    private Long weekHeaderId;
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyCurriculumItem> items = new ArrayList<>();
}
