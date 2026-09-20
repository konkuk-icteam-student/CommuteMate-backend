package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkAttendanceService {
    private final WorkAttendanceRepository workAttendanceRepository;

    @Transactional(readOnly = true)
    public List<AttendanceHistoryResponse> getAttendanceHistory(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<WorkAttendance> attendances =
                workAttendanceRepository.findByUser_UserIdAndCheckTimeBetween(userId, start, end);

        return attendances.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    private AttendanceHistoryResponse toHistoryResponse(WorkAttendance attendance) {
        return AttendanceHistoryResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .checkTime(attendance.getCheckTime())
                .checkType(attendance.getCheckTypeCode())
                .scheduleId(attendance.getSchedule().getScheduleId())
                .scheduleStartTime(attendance.getSchedule().getStartTime())
                .scheduleEndTime(attendance.getSchedule().getEndTime())
                .build();
    }

}
