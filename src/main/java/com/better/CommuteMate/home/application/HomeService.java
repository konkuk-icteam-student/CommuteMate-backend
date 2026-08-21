package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AttendanceErrorCode;
import com.better.CommuteMate.global.util.DisplayTimeZoneUtils;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse.AttendanceStatus;
import com.better.CommuteMate.home.controller.dto.HomeCheckInResponse;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import com.better.CommuteMate.home.controller.dto.TodayScheduleResponse;
import com.better.CommuteMate.home.controller.dto.WeeklyWorkSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;

    private static final List<CodeType> VALID_STATUS_CODES = List.of(CodeType.WS01, CodeType.WS02);
    private static final int CHECK_IN_GRACE_MINUTES = 10;

    @Transactional(readOnly = true)
    public TodayScheduleResponse getTodaySchedules(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<WorkSchedule> slots = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(userId, today, today, VALID_STATUS_CODES);
        slots.sort(Comparator.comparing(WorkSchedule::getStartTime));

        if (slots.isEmpty()) {
            return TodayScheduleResponse.builder().date(today).schedules(List.of()).build();
        }

        Map<Long, WorkAttendance> checkInByScheduleId = workAttendanceRepository
                .findAllByScheduleIn(slots).stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .collect(Collectors.toMap(
                        a -> a.getSchedule().getScheduleId(),
                        a -> a,
                        (a1, a2) -> a1
                ));

        List<TodayScheduleResponse.ScheduleItem> items = ScheduleSlotUtils.mergeConsecutiveSlots(slots).stream()
                .map(group -> {
                    WorkSchedule first = group.get(0);
                    WorkSchedule last = group.get(group.size() - 1);
                    List<Long> scheduleIds = group.stream().map(WorkSchedule::getScheduleId).toList();

                    Optional<WorkAttendance> earliest = group.stream()
                            .map(s -> checkInByScheduleId.get(s.getScheduleId()))
                            .filter(Objects::nonNull)
                            .min(Comparator.comparing(WorkAttendance::getCheckTime));

                    boolean checkedIn = earliest.isPresent();
                    return TodayScheduleResponse.ScheduleItem.builder()
                            .scheduleIds(scheduleIds)
                            .label(first.getStartTime().isBefore(LocalTime.NOON) ? "오전 근무" : "오후 근무")
                            .start(first.getStartTime())
                            .end(last.getEndTime())
                            .workStatusCode(resolveWorkStatusCode(
                                    today, first.getStartTime(), last.getEndTime(), checkedIn, now))
                            .checkedIn(checkedIn)
                            .checkInTime(checkedIn ? earliest.get().getCheckTime() : null)
                            .build();
                })
                .toList();

        return TodayScheduleResponse.builder().date(today).schedules(items).build();
    }

    @Transactional
    public HomeCheckInResponse checkIn(Long userId, List<Long> scheduleIds) {
        List<WorkSchedule> schedules = workSchedulesRepository.findAllById(scheduleIds);
        LocalDate today = LocalDate.now();

        if (schedules.size() != scheduleIds.size()
                || schedules.stream().anyMatch(s -> !s.getUser().getUserId().equals(userId))
                || schedules.stream().anyMatch(s -> !s.getDate().equals(today))
                || schedules.stream().anyMatch(s -> s.getStatusCode() == CodeType.WS04)) {
            throw CustomException.of(AttendanceErrorCode.SCHEDULE_NOT_FOUND);
        }

        schedules.sort(Comparator.comparing(WorkSchedule::getStartTime));

        if (!ScheduleSlotUtils.isConsecutive(schedules)) {
            throw CustomException.of(AttendanceErrorCode.NOT_CONSECUTIVE_SLOTS);
        }

        boolean alreadyCheckedIn = workAttendanceRepository.findAllByScheduleIn(schedules).stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);
        if (alreadyCheckedIn) {
            throw CustomException.of(AttendanceErrorCode.ALREADY_CHECKED_IN);
        }

        LocalDateTime now = LocalDateTime.now();
        WorkSchedule firstSlot = schedules.get(0);
        LocalDateTime lateThreshold = LocalDateTime.of(firstSlot.getDate(), firstSlot.getStartTime())
                .plusMinutes(CHECK_IN_GRACE_MINUTES);
        if (now.isAfter(lateThreshold)) {
            throw CustomException.of(AttendanceErrorCode.CHECK_IN_LATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        List<WorkAttendance> records = schedules.stream()
                .map(s -> WorkAttendance.builder()
                        .user(user)
                        .schedule(s)
                        .checkTime(now)
                        .checkTypeCode(CodeType.CT01)
                        .verified(true)
                        .build())
                .toList();
        workAttendanceRepository.saveAll(records);

        return HomeCheckInResponse.builder()
                .scheduleIds(scheduleIds)
                // [임시] 전역 타임존(UTC) 미해결로 인한 출력 KST 보정. 전역 타임존 KST 전환 시 제거할 것.
                // 주의: 위 WorkAttendance 저장(checkTime)과 지각 판정(lateThreshold)은 now(UTC)를
                // 그대로 써야 한다 — 이 응답 지점에서만 보정해서 넣는다.
                .checkInTime(DisplayTimeZoneUtils.toKstForDisplay(now))
                .build();
    }

    private String resolveWorkStatusCode(LocalDate date, LocalTime mergedStart, LocalTime mergedEnd,
                                          boolean checkedIn, LocalDateTime now) {
        LocalDateTime end = LocalDateTime.of(date, mergedEnd);
        LocalDateTime lateThreshold = LocalDateTime.of(date, mergedStart).plusMinutes(CHECK_IN_GRACE_MINUTES);
        if (checkedIn) {
            return now.isBefore(end) ? CodeType.WK02.getFullCode() : CodeType.WK03.getFullCode();
        } else {
            return now.isAfter(lateThreshold) ? CodeType.WK04.getFullCode() : CodeType.WK01.getFullCode();
        }
    }

    /**
     * 오늘의 총 근무 시간(분 단위)과 예정된 스케줄 개수를 조회합니다.
     */
    @Transactional(readOnly = true)
    public HomeWorkTimeResponse getTodayWorkTime(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        today,
                        today,
                        VALID_STATUS_CODES
                );

        long totalMinutes = 0;

        for (WorkSchedule schedule : schedules) {
            List<WorkAttendance> attendances =
                    workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

            totalMinutes += calculateWorkMinutes(schedule, attendances, now);
        }

        return HomeWorkTimeResponse.builder()
                .totalMinutes(totalMinutes)
                .scheduleCount(schedules.size())
                .build();
    }

    /**
     * 현재 시각 기준 사용자의 출퇴근 상태를 판별합니다.
     */
    @Transactional(readOnly = true)
    public HomeAttendanceStatusResponse getAttendanceStatus(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        today,
                        today,
                        VALID_STATUS_CODES
                );

        if (schedules.isEmpty()) {
            return HomeAttendanceStatusResponse.builder()
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .message("오늘 예정된 근무가 없습니다.")
                    .build();
        }

        WorkSchedule targetSchedule = findRelevantSchedule(schedules, now);

        if (targetSchedule == null) {
            schedules.sort(Comparator.comparing(WorkSchedule::getStartTime));
            targetSchedule = schedules.get(schedules.size() - 1);

            LocalDateTime targetEndTime = toDateTime(targetSchedule, targetSchedule.getEndTime());

            if (now.isAfter(targetEndTime.plusHours(1))) {
                return HomeAttendanceStatusResponse.builder()
                        .status(AttendanceStatus.COMPLETED)
                        .message("오늘의 모든 근무가 종료되었습니다.")
                        .build();
            }
        }

        return determineStatus(targetSchedule, now);
    }

    /**
     * 이번 주 및 이번 달 근무 시간 요약 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public WeeklyWorkSummaryResponse getWorkSummary(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        List<WorkSchedule> weeklySchedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        weekStart,
                        weekEnd,
                        VALID_STATUS_CODES
                );

        List<WorkSchedule> monthlySchedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        monthStart,
                        monthEnd,
                        VALID_STATUS_CODES
                );

        double totalWeeklyHours = calculateTotalHours(weeklySchedules);
        double completedWeeklyHours = calculateCompletedHours(weeklySchedules);
        double completedMonthlyHours = calculateCompletedHours(monthlySchedules);

        return WeeklyWorkSummaryResponse.builder()
                .totalWeeklyHours(totalWeeklyHours)
                .completedWeeklyHours(completedWeeklyHours)
                .completedMonthlyHours(completedMonthlyHours)
                .build();
    }

    private long calculateWorkMinutes(
            WorkSchedule schedule,
            List<WorkAttendance> attendances,
            LocalDateTime now
    ) {
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

        LocalDateTime scheduleStartTime = toDateTime(schedule, schedule.getStartTime());
        LocalDateTime scheduleEndTime = toDateTime(schedule, schedule.getEndTime());

        LocalDateTime start = checkInTime.get();
        LocalDateTime end = checkOutTime.orElse(now);

        if (start.isBefore(scheduleStartTime)) {
            start = scheduleStartTime;
        }

        if (end.isAfter(scheduleEndTime)) {
            end = scheduleEndTime;
        }

        if (start.isAfter(end)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes();
    }

    private WorkSchedule findRelevantSchedule(List<WorkSchedule> schedules, LocalDateTime now) {
        schedules.sort(Comparator.comparing(WorkSchedule::getStartTime));

        for (WorkSchedule schedule : schedules) {
            LocalDateTime start = toDateTime(schedule, schedule.getStartTime());
            LocalDateTime end = toDateTime(schedule, schedule.getEndTime());

            if (now.isAfter(start.minusMinutes(10)) && now.isBefore(end.plusHours(1))) {
                return schedule;
            }
        }

        for (WorkSchedule schedule : schedules) {
            LocalDateTime start = toDateTime(schedule, schedule.getStartTime());

            if (now.isBefore(start)) {
                return schedule;
            }
        }

        return null;
    }

    private HomeAttendanceStatusResponse determineStatus(WorkSchedule schedule, LocalDateTime now) {
        List<WorkAttendance> attendances =
                workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

        boolean hasCheckIn = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);

        boolean hasCheckOut = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);

        LocalDateTime scheduleStartTime = toDateTime(schedule, schedule.getStartTime());
        LocalDateTime scheduleEndTime = toDateTime(schedule, schedule.getEndTime());

        AttendanceStatus status;
        String message;

        if (hasCheckOut) {
            status = AttendanceStatus.COMPLETED;
            message = "근무가 종료되었습니다.";
        } else if (hasCheckIn) {
            if (now.isAfter(scheduleEndTime.minusMinutes(5))) {
                status = AttendanceStatus.CAN_CHECK_OUT;
                message = "퇴근 체크가 가능합니다.";
            } else {
                status = AttendanceStatus.WORKING;
                message = "근무 중입니다.";
            }
        } else {
            if (now.isAfter(scheduleStartTime)) {
                status = AttendanceStatus.LATE_CHECK_IN;
                message = "지각입니다. 서둘러 출근하세요!";
            } else if (now.isAfter(scheduleStartTime.minusMinutes(10))) {
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
     * 스케줄 리스트의 전체 근무 시간을 계산합니다.
     */
    private double calculateTotalHours(List<WorkSchedule> schedules) {
        long totalMinutes = schedules.stream()
                .mapToLong(schedule -> Duration.between(
                        schedule.getStartTime(),
                        schedule.getEndTime()
                ).toMinutes())
                .sum();

        return totalMinutes / 60.0;
    }

    /**
     * 스케줄 리스트 중 퇴근 체크가 완료된 것만의 근무 시간을 계산합니다.
     */
    private double calculateCompletedHours(List<WorkSchedule> schedules) {
        long totalMinutes = schedules.stream()
                .collect(Collectors.groupingBy(WorkSchedule::getDate))
                .values().stream()
                .flatMap(daySchedules -> {
                    List<WorkSchedule> sorted = new ArrayList<>(daySchedules);
                    sorted.sort(Comparator.comparing(WorkSchedule::getStartTime));
                    return ScheduleSlotUtils.mergeConsecutiveSlots(sorted).stream();
                })
                .filter(group -> group.stream().anyMatch(this::hasCheckOut))
                .flatMap(List::stream)
                .mapToLong(schedule -> Duration.between(
                        schedule.getStartTime(), schedule.getEndTime()).toMinutes())
                .sum();

        return totalMinutes / 60.0;
    }

    /**
     * 해당 스케줄에 퇴근 체크 기록이 있는지 확인합니다.
     */
    private boolean hasCheckOut(WorkSchedule schedule) {
        List<WorkAttendance> attendances =
                workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

        return attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);
    }

    private LocalDateTime toDateTime(WorkSchedule schedule, java.time.LocalTime time) {
        return LocalDateTime.of(schedule.getDate(), time);
    }
}
