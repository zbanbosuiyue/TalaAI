package com.tala.origindata.repository;

import com.tala.origindata.domain.DailyCurriculumDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Daily Curriculum Day
 */
@Repository
public interface DailyCurriculumDayRepository extends JpaRepository<DailyCurriculumDay, Long> {
    
    List<DailyCurriculumDay> findBySchoolIdAndClassroomIdOrderByDateDesc(
        Long schoolId, Long classroomId);
    
    List<DailyCurriculumDay> findBySchoolIdAndClassroomIdAndDateBetweenOrderByDateDesc(
        Long schoolId, Long classroomId, LocalDate startDate, LocalDate endDate);
    
    Optional<DailyCurriculumDay> findByOriginDataId(Long originDataId);
    
    Optional<DailyCurriculumDay> findBySchoolIdAndClassroomIdAndDate(
        Long schoolId, Long classroomId, LocalDate date);
    
    List<DailyCurriculumDay> findByWeekHeaderId(Long weekHeaderId);
    
    long countBySchoolIdAndClassroomId(Long schoolId, Long classroomId);
}
