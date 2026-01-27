package com.better.CommuteMate.domain.task.repository;

import com.better.CommuteMate.domain.task.entity.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    /**
     * 활성화된 템플릿 목록 조회
     */
    List<TaskTemplate> findByIsActiveTrueOrderByTemplateNameAsc();

    /**
     * 모든 템플릿 목록 조회 (이름순)
     */
    List<TaskTemplate> findAllByOrderByTemplateNameAsc();

    /**
     * 템플릿 상세 조회 (항목 포함)
     */
    @Query("SELECT t FROM TaskTemplate t LEFT JOIN FETCH t.items WHERE t.templateId = :templateId")
    Optional<TaskTemplate> findByIdWithItems(@Param("templateId") Long templateId);

    /**
     * 템플릿 이름 중복 확인
     */
    boolean existsByTemplateName(String templateName);

    /**
     * 다른 템플릿에서 같은 이름 사용 여부 확인 (수정 시)
     */
    @Query("SELECT COUNT(t) > 0 FROM TaskTemplate t WHERE t.templateName = :templateName AND t.templateId != :templateId")
    boolean existsByTemplateNameAndNotId(
            @Param("templateName") String templateName,
            @Param("templateId") Long templateId);
}
