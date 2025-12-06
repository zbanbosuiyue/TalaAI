package com.tala.origindata.repository;

import com.tala.origindata.domain.DailyCurriculumItem;
import com.tala.origindata.constant.LearningDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Daily Curriculum Item
 */
@Repository
public interface DailyCurriculumItemRepository extends JpaRepository<DailyCurriculumItem, Long> {
    
    List<DailyCurriculumItem> findByDayIdOrderByDisplayOrderAsc(Long dayId);
    
    List<DailyCurriculumItem> findByDayIdAndDomainOrderByDisplayOrderAsc(
        Long dayId, LearningDomain domain);
    
    long countByDayId(Long dayId);
}
