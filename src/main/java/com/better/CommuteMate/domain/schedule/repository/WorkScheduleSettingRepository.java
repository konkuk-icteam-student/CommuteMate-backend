package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 동일 월의 동시 승인 요청이 같은 정원 값을 보고 모두 통과하지 않도록 설정 행을 잠급니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select setting
            from WorkScheduleSetting setting
            where setting.organizationId = :organizationId
              and setting.year = :year
              and setting.month = :month
            """)
    Optional<WorkScheduleSetting> findForUpdate(
            @Param("organizationId") String organizationId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
