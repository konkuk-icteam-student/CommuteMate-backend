package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyScheduleLimitRepository extends JpaRepository<MonthlyScheduleLimit, Integer> {

    Optional<MonthlyScheduleLimit> findByScheduleYearAndScheduleMonth(Integer scheduleYear, Integer scheduleMonth);
}