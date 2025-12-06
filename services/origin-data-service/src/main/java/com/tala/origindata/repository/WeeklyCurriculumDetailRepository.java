package com.tala.origindata.repository;

import com.tala.origindata.domain.WeeklyCurriculumDetail;
import com.tala.origindata.constant.CurriculumScope;
import com.tala.origindata.constant.LearningDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Weekly Curriculum Detail
 */
@Repository
public interface WeeklyCurriculumDetailRepository extends JpaRepository<WeeklyCurriculumDetail, Long> {
    
    List<WeeklyCurriculumDetail> findByHeaderIdOrderByDisplayOrderAsc(Long headerId);
    
    List<WeeklyCurriculumDetail> findByHeaderIdAndScopeOrderByDisplayOrderAsc(
        Long headerId, CurriculumScope scope);
    
    List<WeeklyCurriculumDetail> findByHeaderIdAndDayOfWeekOrderByDisplayOrderAsc(
        Long headerId, Integer dayOfWeek);
    
    List<WeeklyCurriculumDetail> findByHeaderIdAndLearningDomainOrderByDisplayOrderAsc(
        Long headerId, LearningDomain learningDomain);
    
    long countByHeaderId(Long headerId);
}
