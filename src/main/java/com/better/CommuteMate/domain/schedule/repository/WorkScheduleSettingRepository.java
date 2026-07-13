package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkScheduleSettingRepository extends JpaRepository<WorkScheduleSetting, String> {

    /**
     * 특정 조직의 특정 연/월 근무 일정 설정을 조회
     */
    Optional<WorkScheduleSetting> findByOrganizationIdAndYearAndMonth(
            String organizationId,
            Integer year,
            Integer month
    );
}