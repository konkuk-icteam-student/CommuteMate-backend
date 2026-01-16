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
import com.better.CommuteMate.home.controller.dto.WeeklyWorkSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;

    /**
     * 오늘의 총 근무 시간(분 단위)과 예정된 스케줄 개수를 조회합니다.
     * <p>
     * - 현재 사용자의 오늘(00:00 ~ 23:59) 유효한 근무 일정을 조회합니다.
     * - 각 스케줄에 연결된 출퇴근 기록(WorkAttendance)을 기반으로 실제 근무 시간을 계산합니다.
     * </p>
     *
     * @param userId 조회를 요청한 사용자의 ID
     * @return {@link HomeWorkTimeResponse} (총 근무 분, 스케줄 개수)
     * @throws BasicException 사용자를 찾을 수 없는 경우
     */
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

    /**
     * 현재 시각 기준 사용자의 출퇴근 상태를 판별합니다.
     * <p>
     * - 오늘 예정된 근무가 없으면 {@code NO_SCHEDULE}을 반환합니다.
     * - 현재 시각과 가장 연관성 있는 스케줄을 찾아 상태를 결정합니다.
     *   - 근무 시작 10분 전: {@code CAN_CHECK_IN}
     *   - 근무 종료 5분 전 ~ 1시간 후: {@code CAN_CHECK_OUT}
     *   - 지각 여부 등도 판별합니다.
     * </p>
     *
     * @param userId 상태를 조회할 사용자의 ID
     * @return {@link HomeAttendanceStatusResponse} (상태 코드, 메시지, 스케줄 정보)
     * @throws BasicException 사용자를 찾을 수 없는 경우
     */
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

    /**
     * 이번 주 및 이번 달 근무 시간 요약 정보를 조회합니다.
     * <p>
     * - 이번 주: 월요일 00:00 ~ 일요일 23:59
     * - 이번 달: 1일 00:00 ~ 말일 23:59
     * - 완료 여부: 퇴근 체크(CT02) 기록이 있는 스케줄만 완료로 간주
     * - 시간 단위: 0.5 = 30분 (예: 3.0, 3.5, 4.0)
     * </p>
     *
     * @param userId 조회를 요청한 사용자의 ID
     * @return {@link WeeklyWorkSummaryResponse} (주간 전체/완료, 월간 완료 시간)
     * @throws BasicException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public WeeklyWorkSummaryResponse getWorkSummary(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // 이번 주 기간 계산 (월요일 00:00 ~ 일요일 23:59)
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
        LocalDateTime weekEnd = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(23, 59, 59);

        // 이번 달 기간 계산 (1일 00:00 ~ 말일 23:59)
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        LocalDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59, 59);

        // 이번 주 전체 스케줄 조회 (승인된 것만)
        List<WorkSchedule> weeklySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, weekStart, weekEnd.plusSeconds(1));

        // 이번 달 전체 스케줄 조회 (승인된 것만)
        List<WorkSchedule> monthlySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, monthStart, monthEnd.plusSeconds(1));

        // 이번 주 전체 근무 시간 계산
        double totalWeeklyHours = calculateTotalHours(weeklySchedules);

        // 이번 주 완료된 근무 시간 계산 (퇴근 체크된 것만)
        double completedWeeklyHours = calculateCompletedHours(weeklySchedules);

        // 이번 달 완료된 근무 시간 계산 (퇴근 체크된 것만)
        double completedMonthlyHours = calculateCompletedHours(monthlySchedules);

        return WeeklyWorkSummaryResponse.builder()
                .totalWeeklyHours(totalWeeklyHours)
                .completedWeeklyHours(completedWeeklyHours)
                .completedMonthlyHours(completedMonthlyHours)
                .build();
    }

    /**
     * 스케줄 리스트의 전체 근무 시간을 계산합니다 (시간 단위).
     */
    private double calculateTotalHours(List<WorkSchedule> schedules) {
        long totalMinutes = schedules.stream()
                .mapToLong(schedule -> Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes())
                .sum();

        return totalMinutes / 60.0;
    }

    /**
     * 스케줄 리스트 중 퇴근 체크가 완료된 것만의 근무 시간을 계산합니다 (시간 단위).
     */
    private double calculateCompletedHours(List<WorkSchedule> schedules) {
        long totalMinutes = schedules.stream()
                .filter(this::hasCheckOut)
                .mapToLong(schedule -> Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes())
                .sum();

        return totalMinutes / 60.0;
    }

    /**
     * 해당 스케줄에 퇴근 체크 기록이 있는지 확인합니다.
     */
    private boolean hasCheckOut(WorkSchedule schedule) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        return attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);
    }
}
