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

        int currentWorkingCount = (int) schedulesByUser.values().stream()
                .filter(userSchedules -> userSchedules.stream()
                        .anyMatch(schedule -> hasCheckInWithoutCheckOut(
                                attendanceBySchedule.getOrDefault(schedule.getScheduleId(), List.of())
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

    private boolean hasCheckInWithoutCheckOut(List<WorkAttendance> attendances) {
        boolean checkedIn = hasCheckIn(attendances);
        boolean checkedOut = attendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT02);
        return checkedIn && !checkedOut;
    }

    private boolean isLate(WorkSchedule schedule, List<WorkAttendance> attendances) {
        LocalDateTime scheduledStart =
                LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        return attendances.stream()
                .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .anyMatch(checkIn -> checkIn.isAfter(scheduledStart.plusMinutes(10)));
    }
}
