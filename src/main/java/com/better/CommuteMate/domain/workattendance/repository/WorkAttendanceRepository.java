package com.better.CommuteMate.domain.workattendance.repository;

import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WorkAttendanceRepository extends JpaRepository<WorkAttendance, String> {
    List<WorkAttendance> findByUser_UserIdAndCheckTime(String userId, Instant checkTime);
    List<WorkAttendance> findBySchedule_ScheduleId(String scheduleId);
}