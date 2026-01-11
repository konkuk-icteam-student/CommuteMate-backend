package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse.AttendanceStatus;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public HomeWorkTimeResponse getTodayWorkTime(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<WorkSchedule> schedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, startOfDay, endOfDay);

        long totalMinutes = 0;

        for (WorkSchedule schedule : schedules) {
            List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
            totalMinutes += calculateWorkMinutes(schedule, attendances, now);
        }

        return HomeWorkTimeResponse.builder()
                .totalMinutes(totalMinutes)
                .scheduleCount(schedules.size())
                .build();
    }

    @Transactional(readOnly = true)
    public HomeAttendanceStatusResponse getAttendanceStatus(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<WorkSchedule> schedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, startOfDay, endOfDay);

        if (schedules.isEmpty()) {
            return HomeAttendanceStatusResponse.builder()
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .message("오늘 예정된 근무가 없습니다.")
                    .build();
        }

        WorkSchedule targetSchedule = findRelevantSchedule(schedules, now);
        
        if (targetSchedule == null) {
            targetSchedule = schedules.get(schedules.size() - 1);
            if (now.isAfter(targetSchedule.getEndTime().plusHours(1))) {
                 return HomeAttendanceStatusResponse.builder()
                    .status(AttendanceStatus.COMPLETED)
                    .message("오늘의 모든 근무가 종료되었습니다.")
                    .build();
            }
        }

        return determineStatus(targetSchedule, now);
    }

    private long calculateWorkMinutes(WorkSchedule schedule, List<WorkAttendance> attendances, LocalDateTime now) {
        Optional<LocalDateTime> checkInTime = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        Optional<LocalDateTime> checkOutTime = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        if (checkInTime.isEmpty()) {
            return 0;
        }

        LocalDateTime start = checkInTime.get();
        LocalDateTime end = checkOutTime.orElse(now);

        if (start.isBefore(schedule.getStartTime())) {
            start = schedule.getStartTime();
        }
        if (end.isAfter(schedule.getEndTime())) {
            end = schedule.getEndTime();
        }

        if (start.isAfter(end)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes();
    }

    private WorkSchedule findRelevantSchedule(List<WorkSchedule> schedules, LocalDateTime now) {
        schedules.sort(Comparator.comparing(WorkSchedule::getStartTime));

        for (WorkSchedule schedule : schedules) {
            LocalDateTime start = schedule.getStartTime();
            LocalDateTime end = schedule.getEndTime();

            if (now.isAfter(start.minusMinutes(10)) && now.isBefore(end.plusHours(1))) {
                return schedule;
            }
        }

        for (WorkSchedule schedule : schedules) {
            if (now.isBefore(schedule.getStartTime())) {
                return schedule;
            }
        }

        return null;
    }

    private HomeAttendanceStatusResponse determineStatus(WorkSchedule schedule, LocalDateTime now) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        boolean hasCheckIn = attendances.stream().anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);
        boolean hasCheckOut = attendances.stream().anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);

        AttendanceStatus status;
        String message;

        if (hasCheckOut) {
            status = AttendanceStatus.COMPLETED;
            message = "근무가 종료되었습니다.";
        } else if (hasCheckIn) {
            if (now.isAfter(schedule.getEndTime().minusMinutes(5))) {
                status = AttendanceStatus.CAN_CHECK_OUT;
                message = "퇴근 체크가 가능합니다.";
            } else {
                status = AttendanceStatus.WORKING;
                message = "근무 중입니다.";
            }
        } else {
            if (now.isAfter(schedule.getStartTime())) {
                status = AttendanceStatus.LATE_CHECK_IN;
                message = "지각입니다. 서둘러 출근하세요!";
            } else if (now.isAfter(schedule.getStartTime().minusMinutes(10))) {
                status = AttendanceStatus.CAN_CHECK_IN;
                message = "출근 체크가 가능합니다.";
            } else {
                status = AttendanceStatus.BEFORE_WORK;
                message = "출근 전입니다.";
            }
        }

        return HomeAttendanceStatusResponse.builder()
                .status(status)
                .message(message)
                .currentScheduleId(schedule.getScheduleId())
                .scheduleStartTime(schedule.getStartTime())
                .scheduleEndTime(schedule.getEndTime())
                .build();
    }
}
