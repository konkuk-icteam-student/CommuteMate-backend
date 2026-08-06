package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkUnavailableTimeRepository extends JpaRepository<WorkUnavailableTime, Long> {

    /**
     * 특정 설정(연/월)에 해당하는 신청 불가 시간 목록 조회
     */
    List<WorkUnavailableTime> findBySettingAndDateBetween(
            WorkScheduleSetting setting,
            LocalDate startDate,
            LocalDate endDate
    );

    void deleteAllBySetting(WorkScheduleSetting setting);
}
