package com.better.CommuteMate.domain.task.repository;

import com.better.CommuteMate.domain.task.entity.TaskTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTemplateItemRepository extends JpaRepository<TaskTemplateItem, Long> {

    /**
     * 특정 템플릿의 항목 조회 (정렬순)
     */
    @Query("SELECT i FROM TaskTemplateItem i LEFT JOIN FETCH i.defaultAssignee WHERE i.template.templateId = :templateId ORDER BY i.displayOrder ASC, i.taskTime ASC")
    List<TaskTemplateItem> findByTemplateIdWithAssignee(@Param("templateId") Long templateId);

    /**
     * 특정 템플릿의 항목 삭제
     */
    void deleteByTemplate_TemplateId(Long templateId);
}
