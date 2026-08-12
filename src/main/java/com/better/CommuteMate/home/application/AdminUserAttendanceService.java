package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AdminHomeErrorCode;
import com.better.CommuteMate.home.controller.dto.AdminUserAttendancePageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserAttendanceService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkAttendanceRepository attendanceRepository;
    private final WorkScheduleSettingRepository settingRepository;

    public AdminUserAttendancePageResponse getUserAttendance(
            Long organizationId,
            String dateValue,
            String userName,
            Integer pageValue,
            Integer sizeValue
    ) {
        LocalDate date = parseDate(dateValue);
        int page = pageValue == null ? 0 : pageValue;
        int size = sizeValue == null ? 6 : sizeValue;
        if (page < 0 || size < 1) {
            throw CustomException.of(AdminHomeErrorCode.INVALID_PAGE);
        }

        Page<User> users = userRepository
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCase(
                        organizationId,
                        CodeType.RL01,
                        userName == null ? "" : userName.trim(),
                        PageRequest.of(page, size)
                );
        List<Long> userIds = users.getContent().stream().map(User::getUserId).toList();
        Map<Long, UserProfile> profiles = userProfileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));

        YearMonth month = YearMonth.from(date);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<WorkSchedule> schedules = userIds.isEmpty()
                ? List.of()
                : scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                        userIds, monthStart, monthEnd, CodeType.WS02
                );
        List<WorkAttendance> attendances = schedules.isEmpty()
                ? List.of()
                : attendanceRepository.findAllByScheduleIn(schedules);
        Map<Long, List<WorkAttendance>> attendancesBySchedule = attendances.stream()
                .collect(Collectors.groupingBy(
                        attendance -> attendance.getSchedule().getScheduleId()
                ));
        Map<Long, List<WorkSchedule>> schedulesByUser = schedules.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getUser().getUserId()));
        Optional<WorkScheduleSetting> setting =
                settingRepository.findByOrganizationIdAndYearAndMonth(
                        organizationId, date.getYear(), date.getMonthValue()
                );
        int weeklyLimit = setting.map(WorkScheduleSetting::getWeeklyMaxMinutes).orElse(0);
        int monthlyLimit = setting.map(WorkScheduleSetting::getMonthlyMaxMinutes).orElse(0);
        LocalDateTime referenceTime = referenceTime(date);

        List<AdminUserAttendancePageResponse.UserAttendance> details = users.getContent().stream()
                .map(user -> {
                    UserProfile profile = profiles.get(user.getUserId());
                    List<WorkSchedule> userSchedules =
                            schedulesByUser.getOrDefault(user.getUserId(), List.of());
                    List<WorkSchedule> dailySchedules = userSchedules.stream()
                            .filter(schedule -> schedule.getDate().equals(date))
                            .toList();
                    Status status = determineStatus(
                            dailySchedules, attendancesBySchedule, referenceTime
                    );
                    LateSummary late = calculateLateSummary(
                            userSchedules, attendancesBySchedule
                    );
                    int weeklyWorked = calculateWorkedMinutes(
                            userSchedules,
                            attendancesBySchedule,
                            schedule -> !schedule.getDate().isBefore(weekStart)
                                    && !schedule.getDate().isAfter(weekEnd),
                            referenceTime
                    );
                    int monthlyWorked = calculateWorkedMinutes(
                            userSchedules,
                            attendancesBySchedule,
                            schedule -> true,
                            referenceTime
                    );
                    return new AdminUserAttendancePageResponse.UserAttendance(
                            String.valueOf(user.getUserId()),
                            user.getName(),
                            profile == null ? null : profile.getDepartment(),
                            profile == null ? null : profile.getStudentId(),
                            status.workStatusCode,
                            status.attendanceStatusCode,
                            late.count,
                            late.minutes,
                            weeklyWorked,
                            weeklyLimit,
                            monthlyWorked,
                            monthlyLimit
                    );
                })
                .toList();

        return new AdminUserAttendancePageResponse(
                date,
                details,
                page,
                size,
                users.getTotalElements(),
                users.getTotalPages()
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

    private Status determineStatus(
            List<WorkSchedule> schedules,
            Map<Long, List<WorkAttendance>> attendancesBySchedule,
            LocalDateTime referenceTime
    ) {
        if (schedules.isEmpty()) {
            return new Status(null, null);
        }
        List<ScheduleStatus> statuses = schedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getStartTime))
                .map(schedule -> scheduleStatus(
                        schedule,
                        attendancesBySchedule.getOrDefault(schedule.getScheduleId(), List.of()),
                        referenceTime
                ))
                .toList();

        return statuses.stream()
                .filter(status -> CodeType.WK02.name().equals(status.workStatusCode))
                .map(ScheduleStatus::toStatus)
                .findFirst()
                .or(() -> statuses.stream()
                        .filter(status -> CodeType.AT03.name().equals(status.attendanceStatusCode))
                        .map(ScheduleStatus::toStatus)
                        .findFirst())
                .or(() -> statuses.stream()
                        .filter(status -> CodeType.WK04.name().equals(status.workStatusCode))
                        .map(ScheduleStatus::toStatus)
                        .findFirst())
                .or(() -> statuses.stream()
                        .filter(status -> CodeType.WK01.name().equals(status.workStatusCode))
                        .map(ScheduleStatus::toStatus)
                        .findFirst())
                .orElseGet(() -> statuses.get(statuses.size() - 1).toStatus());
    }

    private ScheduleStatus scheduleStatus(
            WorkSchedule schedule,
            List<WorkAttendance> attendances,
            LocalDateTime referenceTime
    ) {
        Optional<LocalDateTime> checkIn = attendances.stream()
                .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .min(Comparator.naturalOrder());
        boolean checkedOut = attendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT02);
        LocalDateTime start = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        LocalDateTime end = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());

        if (checkIn.isPresent()) {
            String attendanceCode = checkIn.get().isAfter(start.plusMinutes(10))
                    ? CodeType.AT02.name()
                    : CodeType.AT01.name();
            return new ScheduleStatus(
                    checkedOut ? CodeType.WK03.name() : CodeType.WK02.name(),
                    attendanceCode
            );
        }
        if (referenceTime.isAfter(end)) {
            return new ScheduleStatus(CodeType.WK04.name(), CodeType.AT03.name());
        }
        if (referenceTime.isAfter(start.plusMinutes(10))) {
            return new ScheduleStatus(CodeType.WK04.name(), null);
        }
        return new ScheduleStatus(CodeType.WK01.name(), null);
    }

    private LateSummary calculateLateSummary(
            List<WorkSchedule> schedules,
            Map<Long, List<WorkAttendance>> attendancesBySchedule
    ) {
        int count = 0;
        int minutes = 0;
        for (WorkSchedule schedule : schedules) {
            Optional<LocalDateTime> checkIn = attendancesBySchedule
                    .getOrDefault(schedule.getScheduleId(), List.of()).stream()
                    .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                    .map(WorkAttendance::getCheckTime)
                    .min(Comparator.naturalOrder());
            LocalDateTime start = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
            if (checkIn.isPresent() && checkIn.get().isAfter(start.plusMinutes(10))) {
                count++;
                minutes += (int) Duration.between(start, checkIn.get()).toMinutes();
            }
        }
        return new LateSummary(count, minutes);
    }

    private int calculateWorkedMinutes(
            List<WorkSchedule> schedules,
            Map<Long, List<WorkAttendance>> attendancesBySchedule,
            Predicate<WorkSchedule> include,
            LocalDateTime referenceTime
    ) {
        long total = 0;
        for (WorkSchedule schedule : schedules.stream().filter(include).toList()) {
            List<WorkAttendance> attendances =
                    attendancesBySchedule.getOrDefault(schedule.getScheduleId(), List.of());
            Optional<LocalDateTime> checkIn = attendances.stream()
                    .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT01)
                    .map(WorkAttendance::getCheckTime)
                    .min(Comparator.naturalOrder());
            if (checkIn.isEmpty()) {
                continue;
            }
            LocalDateTime scheduledStart =
                    LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
            LocalDateTime scheduledEnd =
                    LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
            LocalDateTime start = checkIn.get().isBefore(scheduledStart)
                    ? scheduledStart
                    : checkIn.get();
            LocalDateTime end = attendances.stream()
                    .filter(attendance -> attendance.getCheckTypeCode() == CodeType.CT02)
                    .map(WorkAttendance::getCheckTime)
                    .min(Comparator.naturalOrder())
                    .orElse(referenceTime);
            if (end.isAfter(scheduledEnd)) {
                end = scheduledEnd;
            }
            if (end.isAfter(start)) {
                total += Duration.between(start, end).toMinutes();
            }
        }
        return Math.toIntExact(total);
    }

    private record Status(String workStatusCode, String attendanceStatusCode) {}
    private record ScheduleStatus(String workStatusCode, String attendanceStatusCode) {
        private Status toStatus() {
            return new Status(workStatusCode, attendanceStatusCode);
        }
    }
    private record LateSummary(int count, int minutes) {}
}
