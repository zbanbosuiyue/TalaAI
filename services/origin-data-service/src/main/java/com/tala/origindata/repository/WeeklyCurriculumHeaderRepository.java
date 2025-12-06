package com.tala.origindata.repository;

import com.tala.origindata.domain.WeeklyCurriculumHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Weekly Curriculum Header
 */
@Repository
public interface WeeklyCurriculumHeaderRepository extends JpaRepository<WeeklyCurriculumHeader, Long> {
    
    List<WeeklyCurriculumHeader> findBySchoolIdAndClassroomIdOrderByWeekStartDateDesc(
        Long schoolId, Long classroomId);
    
    List<WeeklyCurriculumHeader> findBySchoolIdAndClassroomIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
        Long schoolId, Long classroomId, LocalDate startDate, LocalDate endDate);
    
    Optional<WeeklyCurriculumHeader> findByOriginDataId(Long originDataId);
    
    Optional<WeeklyCurriculumHeader> findBySchoolIdAndClassroomIdAndWeekStartDate(
        Long schoolId, Long classroomId, LocalDate weekStartDate);
    
    long countBySchoolIdAndClassroomId(Long schoolId, Long classroomId);
}
