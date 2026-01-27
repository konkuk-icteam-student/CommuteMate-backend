package com.better.CommuteMate.domain.workattendance.repository;

import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkAttendanceRepository extends JpaRepository<WorkAttendance, Integer> {
    List<WorkAttendance> findByUser_UserIdAndCheckTime(Long userId, LocalDateTime checkTime);
    List<WorkAttendance> findBySchedule_ScheduleId(Long scheduleId);
    List<WorkAttendance> findByUser_UserIdAndCheckTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}