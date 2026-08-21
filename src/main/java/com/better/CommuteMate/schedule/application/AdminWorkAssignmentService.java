package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkAssignmentRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkAssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWorkAssignmentService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final List<CodeType> DUPLICATE_STATUSES =
            List.of(CodeType.WS01, CodeType.WS02);

    private final UserRepository userRepository;
    private final WorkScheduleSettingRepository settingRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkAttendanceRepository attendanceRepository;

    @Transactional
    public AdminWorkAssignmentResponse assign(
            AdminWorkAssignmentRequest request,
            Long organizationId,
            Long adminId
    ) {
        ParsedAssignment parsed = parseAndValidate(request);
        User user = findUser(request.userId(), organizationId);

        WorkScheduleSetting setting = settingRepository.findForUpdate(
                        organizationId,
                        parsed.date().getYear(),
                        parsed.date().getMonthValue()
                )
                .orElseThrow(() -> CustomException.of(
                        ScheduleErrorCode.ADMIN_SCHEDULE_SETTING_NOT_FOUND
                ));
        Workplace workplace = workplaceRepository
                .findFirstByOrganizationId(organizationId)
                .orElseThrow(() -> CustomException.of(
                        ScheduleErrorCode.ADMIN_WORK_ASSIGNMENT_WORKPLACE_NOT_FOUND
                ));

        if (scheduleRepository
                .existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
                        user.getUserId(),
                        parsed.date(),
                        parsed.startTime(),
                        parsed.endTime(),
                        DUPLICATE_STATUSES
                )) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_WORK_ASSIGNMENT_DUPLICATED);
        }

        WorkSchedule schedule = scheduleRepository
                .findFirstByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeOrderByUpdatedAtDesc(
                        user.getUserId(),
                        parsed.date(),
                        parsed.startTime(),
                        parsed.endTime(),
                        CodeType.WS04
                )
                .map(cancelled -> {
                    cancelled.updateSchedule(
                            setting,
                            workplace,
                            parsed.date(),
                            parsed.startTime(),
                            parsed.endTime(),
                            String.valueOf(adminId)
                    );
                    cancelled.updateStatus(CodeType.WS02, String.valueOf(adminId));
                    return cancelled;
                })
                .orElseGet(() -> WorkSchedule.builder()
                        .user(user)
                        .setting(setting)
                        .workplace(workplace)
                        .date(parsed.date())
                        .startTime(parsed.startTime())
                        .endTime(parsed.endTime())
                        .statusCode(CodeType.WS02)
                        .createdBy(String.valueOf(adminId))
                        .updatedBy(String.valueOf(adminId))
                        .build());
        schedule = scheduleRepository.saveAndFlush(schedule);
        createAttendanceForPastAssignment(schedule, user, adminId, LocalDateTime.now());

        long currentCount = scheduleRepository
                .countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                        setting,
                        parsed.date(),
                        parsed.startTime(),
                        parsed.endTime(),
                        CodeType.WS02
        );
        return new AdminWorkAssignmentResponse(
                schedule.getScheduleId(),
                String.valueOf(user.getUserId()),
                user.getName(),
                parsed.date(),
                parsed.startTime(),
                parsed.endTime(),
                currentCount,
                setting.getMaxConcurrentWorkers()
        );
    }

    private User findUser(String userIdValue, Long organizationId) {
        try {
            return userRepository
                    .findByUserIdAndOrganizationId(Long.parseLong(userIdValue), organizationId)
                    .orElseThrow(() -> CustomException.of(
                            ScheduleErrorCode.ADMIN_WORK_ASSIGNMENT_USER_NOT_FOUND
                    ));
        } catch (NumberFormatException e) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_WORK_ASSIGNMENT_USER_NOT_FOUND);
        }
    }

    private void createAttendanceForPastAssignment(
            WorkSchedule schedule,
            User user,
            Long adminId,
            LocalDateTime now
    ) {
        LocalDateTime scheduledEnd = LocalDateTime.of(
                schedule.getDate(), schedule.getEndTime()
        );
        if (!scheduledEnd.isBefore(now)) {
            return;
        }

        List<WorkAttendance> existingAttendances =
                attendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        boolean hasCheckIn = existingAttendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT01);
        boolean hasCheckOut = existingAttendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT02);

        List<WorkAttendance> newAttendances = new ArrayList<>();
        if (!hasCheckIn) {
            newAttendances.add(buildAttendance(
                    schedule, user, CodeType.CT01,
                    LocalDateTime.of(schedule.getDate(), schedule.getStartTime()), adminId
            ));
        }
        if (!hasCheckOut) {
            newAttendances.add(buildAttendance(
                    schedule, user, CodeType.CT02, scheduledEnd, adminId
            ));
        }
        if (!newAttendances.isEmpty()) {
            attendanceRepository.saveAll(newAttendances);
        }
    }

    private WorkAttendance buildAttendance(
            WorkSchedule schedule,
            User user,
            CodeType checkTypeCode,
            LocalDateTime checkTime,
            Long adminId
    ) {
        return WorkAttendance.builder()
                .user(user)
                .schedule(schedule)
                .checkTime(checkTime)
                .checkTypeCode(checkTypeCode)
                .verified(true)
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();
    }

    private ParsedAssignment parseAndValidate(AdminWorkAssignmentRequest request) {
        try {
            LocalDate date = LocalDate.parse(request.date(), DATE_FORMATTER);
            LocalTime startTime = LocalTime.parse(request.startTime(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(request.endTime(), TIME_FORMATTER);
            if (startTime.getMinute() % 30 != 0
                    || !endTime.equals(startTime.plusMinutes(30))) {
                throw invalidTime();
            }
            return new ParsedAssignment(date, startTime, endTime);
        } catch (DateTimeParseException e) {
            throw invalidTime();
        }
    }

    private CustomException invalidTime() {
        return CustomException.of(ScheduleErrorCode.INVALID_ADMIN_WORK_ASSIGNMENT_TIME);
    }

    private record ParsedAssignment(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
