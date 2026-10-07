package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AdminHomeErrorCode;
import com.better.CommuteMate.home.controller.dto.AdminAttendanceSummaryResponse;
import com.better.CommuteMate.global.util.AttendancePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHomeService {

    private final WorkSchedulesRepository scheduleRepository;
    private final WorkAttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;

    public AdminAttendanceSummaryResponse getAttendanceSummary(
            Long organizationId,
            String dateValue
    ) {
        LocalDate date = parseDate(dateValue);
        List<WorkSchedule> schedules =
                scheduleRepository.findAllByUser_OrganizationIdAndDateAndStatusCode(
                        organizationId,
                        date,
                        CodeType.WS02
                );
        List<WorkAttendance> attendances = schedules.isEmpty()
                ? List.of()
                : attendanceRepository.findAllByScheduleIn(schedules);
        Map<Long, List<WorkAttendance>> attendanceBySchedule = attendances.stream()
                .collect(Collectors.groupingBy(
                        attendance -> attendance.getSchedule().getScheduleId()
                ));
        Map<Long, List<WorkSchedule>> schedulesByUser = schedules.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getUser().getUserId()));
        LocalDateTime referenceTime = referenceTime(date);

        int currentWorkingCount = (int) schedulesByUser.values().stream()
                .filter(userSchedules -> userSchedules.stream()
                        .anyMatch(schedule -> isCurrentlyWorking(
                                schedule,
                                attendanceBySchedule.getOrDefault(schedule.getScheduleId(), List.of()),
                                referenceTime
                        )))
                .count();
        int notCheckedInCount = (int) schedulesByUser.values().stream()
                .filter(userSchedules -> userSchedules.stream()
                        .noneMatch(schedule -> hasCheckIn(
                                attendanceBySchedule.getOrDefault(schedule.getScheduleId(), List.of())
                        )))
                .count();
        int lateCount = (int) schedulesByUser.values().stream()
                .filter(userSchedules -> userSchedules.stream()
                        .anyMatch(schedule -> isLate(
                                schedule,
                                attendanceBySchedule.getOrDefault(schedule.getScheduleId(), List.of())
                        )))
                .count();

        List<Task> tasks =
                taskRepository.findAllByAssignee_OrganizationIdAndTaskDate(organizationId, date);
        int completedTaskCount = (int) tasks.stream()
                .filter(task -> Boolean.TRUE.equals(task.getIsCompleted()))
                .count();

        return new AdminAttendanceSummaryResponse(
                date,
                currentWorkingCount,
                notCheckedInCount,
                lateCount,
                new AdminAttendanceSummaryResponse.TodayTask(
                        completedTaskCount,
                        tasks.size()
                )
        );
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw CustomException.of(AdminHomeErrorCode.INVALID_DATE);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(AdminHomeErrorCode.INVALID_DATE);
        }
    }

    private boolean hasCheckIn(List<WorkAttendance> attendances) {
        return attendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT01);
    }

    private boolean isCurrentlyWorking(
            WorkSchedule schedule,
            List<WorkAttendance> attendances,
            LocalDateTime referenceTime
    ) {
        LocalDateTime scheduledStart = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        LocalDateTime scheduledEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
        boolean checkedIn = attendances.stream()
                .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                .anyMatch(attendance -> !attendance.getCheckTime().isAfter(referenceTime));
        boolean checkedOut = attendances.stream()
                .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT02)
                .anyMatch(attendance -> !attendance.getCheckTime().isAfter(referenceTime));

        return checkedIn
                && !checkedOut
                && !referenceTime.isBefore(scheduledStart)
                && referenceTime.isBefore(scheduledEnd);
    }

    private LocalDateTime referenceTime(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return date.plusDays(1).atStartOfDay();
        }
        if (date.isAfter(today)) {
            return date.atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private boolean isLate(WorkSchedule schedule, List<WorkAttendance> attendances) {
        LocalDateTime scheduledStart =
                LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        return attendances.stream()
                .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .anyMatch(checkIn -> AttendancePolicy.isLate(scheduledStart, checkIn));
    }
}
