package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));
        return new UserInfoResponse(user);
    }

    @Transactional(readOnly = true)
    public UserWorkTimeResponse getWeeklyWorkTime(Integer userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(WeekFields.ISO.dayOfWeek(), 1);
        LocalDateTime start = startOfWeek.atStartOfDay();
        LocalDateTime end = startOfWeek.plusDays(7).atStartOfDay();

        long minutes = calculateTotalWorkTime(userId, start, end);
        return new UserWorkTimeResponse(minutes, "WEEKLY");
    }

    @Transactional(readOnly = true)
    public UserWorkTimeResponse getMonthlyWorkTime(Integer userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime start = startOfMonth.atStartOfDay();
        LocalDateTime end = startOfMonth.plusMonths(1).atStartOfDay();

        long minutes = calculateTotalWorkTime(userId, start, end);
        return new UserWorkTimeResponse(minutes, "MONTHLY");
    }

    private long calculateTotalWorkTime(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<WorkSchedule> schedules = workSchedulesRepository.findAllSchedulesByUserAndDateRange(
                userId, start, end);
        
        long totalMinutes = 0;
        for (WorkSchedule schedule : schedules) {
            if (schedule.getStatusCode() != CodeType.WS02) {
                continue;
            }

            List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
            totalMinutes += calculateDuration(schedule, attendances);
        }
        return totalMinutes;
    }

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

        LocalDateTime start = checkIn.get();
        LocalDateTime end = checkOut.orElse(LocalDateTime.now());

        if (start.isBefore(schedule.getStartTime())) start = schedule.getStartTime();
        if (end.isAfter(schedule.getEndTime())) end = schedule.getEndTime();

        if (start.isAfter(end)) return 0;

        return Duration.between(start, end).toMinutes();
    }
}
