package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;

    private static final List<CodeType> VALID_STATUS_CODES = List.of(
            CodeType.WS01,
            CodeType.WS02
    );

    /**
     * 사용자의 기본 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        return new UserInfoResponse(user);
    }

    /**
     * 특정 사용자의 주간 근무 시간을 계산합니다.
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getWeeklyWorkTime(Long userId) {
        LocalDate now = LocalDate.now();

        LocalDate start = now.with(WeekFields.ISO.dayOfWeek(), 1);
        LocalDate end = start.plusDays(6);

        long minutes = calculateTotalWorkTime(userId, start, end);

        return new UserWorkTimeResponse(minutes, "WEEKLY");
    }

    /**
     * 특정 사용자의 월간 근무 시간을 계산합니다.
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getMonthlyWorkTime(Long userId) {
        LocalDate now = LocalDate.now();

        LocalDate start = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = now.with(TemporalAdjusters.lastDayOfMonth());

        long minutes = calculateTotalWorkTime(userId, start, end);

        return new UserWorkTimeResponse(minutes, "MONTHLY");
    }

    /**
     * 주어진 기간 동안의 총 근무 시간을 계산하는 내부 메서드입니다.
     */
    private long calculateTotalWorkTime(Long userId, LocalDate start, LocalDate end) {
        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        start,
                        end,
                        VALID_STATUS_CODES
                );

        long totalMinutes = 0;

        for (WorkSchedule schedule : schedules) {
            if (schedule.getStatusCode() != CodeType.WS02) {
                continue;
            }

            List<WorkAttendance> attendances =
                    workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

            totalMinutes += calculateDuration(schedule, attendances);
        }

        return totalMinutes;
    }

    /**
     * 단일 일정에 대한 실제 근무 시간을 계산합니다.
     */
    private long calculateDuration(WorkSchedule schedule, List<WorkAttendance> attendances) {
        Optional<LocalDateTime> checkIn = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        Optional<LocalDateTime> checkOut = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        if (checkIn.isEmpty()) {
            return 0;
        }

        LocalDateTime scheduleStart = toDateTime(schedule, schedule.getStartTime());
        LocalDateTime scheduleEnd = toDateTime(schedule, schedule.getEndTime());

        LocalDateTime start = checkIn.get();
        LocalDateTime end = checkOut.orElse(LocalDateTime.now());

        if (start.isBefore(scheduleStart)) {
            start = scheduleStart;
        }

        if (end.isAfter(scheduleEnd)) {
            end = scheduleEnd;
        }

        if (start.isAfter(end)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes();
    }

    private LocalDateTime toDateTime(WorkSchedule schedule, LocalTime time) {
        return LocalDateTime.of(schedule.getDate(), time);
    }
}