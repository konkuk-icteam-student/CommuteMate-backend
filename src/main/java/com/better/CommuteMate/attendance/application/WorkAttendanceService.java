package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AttendanceErrorCode;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkAttendanceService {

    private final WorkAttendanceRepository workAttendanceRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final UserRepository userRepository;
    private final QrTokenManager qrTokenManager;

    private static final List<CodeType> VALID_STATUS_CODES = List.of(
            CodeType.WS01,
            CodeType.WS02
    );

    /**
     * 관리자용: 출근 인증 QR 토큰 발급
     */
    public QrTokenResponse generateQrToken() {
        String token = qrTokenManager.generateToken();

        return QrTokenResponse.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(60))
                .validSeconds(60)
                .build();
    }

    /**
     * 사용자용: 출근 체크
     */
    @Transactional
    public void checkIn(Long userId, String qrToken) {
        if (!qrTokenManager.validateToken(qrToken)) {
            throw new CustomException(AttendanceErrorCode.INVALID_QR_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        today,
                        today,
                        VALID_STATUS_CODES
                );

        if (schedules.isEmpty()) {
            throw new CustomException(AttendanceErrorCode.NO_SCHEDULE_FOUND);
        }

        WorkSchedule targetSchedule = findTargetScheduleForCheckIn(schedules, now);

        if (targetSchedule == null) {
            throw new CustomException(AttendanceErrorCode.NOT_WORK_TIME);
        }

        List<WorkSchedule> consecutiveSchedules = findConsecutiveGroup(schedules, targetSchedule);
        checkIfAlreadyCheckedIn(consecutiveSchedules);

        List<WorkAttendance> attendances = consecutiveSchedules.stream()
                .map(schedule -> WorkAttendance.builder()
                        .user(user)
                        .schedule(schedule)
                        .checkTime(now)
                        .checkTypeCode(CodeType.CT01)
                        .verified(true)
                        .build())
                .toList();

        workAttendanceRepository.saveAll(attendances);
        WorkSchedule firstSchedule = consecutiveSchedules.get(0);
        boolean late = now.isAfter(toDateTime(firstSchedule, firstSchedule.getStartTime()).plusMinutes(10));
        consecutiveSchedules.forEach(schedule -> schedule.markWorking(late, String.valueOf(userId)));
    }

    /**
     * 사용자용: 퇴근 체크
     */
    @Transactional
    public void checkOut(Long userId, String qrToken) {
        if (!qrTokenManager.validateToken(qrToken)) {
            throw new CustomException(AttendanceErrorCode.INVALID_QR_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        today,
                        today,
                        VALID_STATUS_CODES
                );

        if (schedules.isEmpty()) {
            throw new CustomException(AttendanceErrorCode.NO_SCHEDULE_FOUND);
        }

        WorkSchedule targetSchedule = findTargetScheduleForCheckOut(schedules, now);

        if (targetSchedule == null) {
            throw new CustomException(AttendanceErrorCode.NOT_WORK_TIME);
        }

        List<WorkSchedule> consecutiveSchedules = findConsecutiveGroup(schedules, targetSchedule);
        if (!hasCheckedIn(consecutiveSchedules)) {
            throw new CustomException(AttendanceErrorCode.CHECK_IN_REQUIRED);
        }

        checkIfAlreadyCheckedOut(consecutiveSchedules);

        List<WorkAttendance> attendances = consecutiveSchedules.stream()
                .map(schedule -> WorkAttendance.builder()
                        .user(user)
                        .schedule(schedule)
                        .checkTime(now)
                        .checkTypeCode(CodeType.CT02)
                        .verified(true)
                        .build())
                .toList();

        workAttendanceRepository.saveAll(attendances);
        consecutiveSchedules.forEach(schedule -> schedule.markCompleted(String.valueOf(userId)));
    }

    /**
     * 특정 날짜의 출퇴근 이력 조회
     */
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

    private WorkSchedule findTargetScheduleForCheckIn(List<WorkSchedule> schedules, LocalDateTime now) {
        for (WorkSchedule schedule : schedules) {
            LocalDateTime start = toDateTime(schedule, schedule.getStartTime());
            LocalDateTime end = toDateTime(schedule, schedule.getEndTime());

            if (now.isAfter(start.minusMinutes(10)) && now.isBefore(end)) {
                return schedule;
            }
        }

        return null;
    }

    private WorkSchedule findTargetScheduleForCheckOut(List<WorkSchedule> schedules, LocalDateTime now) {
        for (WorkSchedule schedule : schedules) {
            LocalDateTime end = toDateTime(schedule, schedule.getEndTime());

            if (now.isAfter(end.minusMinutes(5)) && now.isBefore(end.plusHours(1))) {
                return schedule;
            }
        }

        return null;
    }

    private void checkIfAlreadyCheckedIn(List<WorkSchedule> schedules) {
        List<WorkAttendance> attendances = workAttendanceRepository.findAllByScheduleIn(schedules);

        boolean exists = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);

        if (exists) {
            throw new CustomException(AttendanceErrorCode.ALREADY_CHECKED_IN);
        }
    }

    private void checkIfAlreadyCheckedOut(List<WorkSchedule> schedules) {
        List<WorkAttendance> attendances = workAttendanceRepository.findAllByScheduleIn(schedules);

        boolean exists = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);

        if (exists) {
            throw new CustomException(AttendanceErrorCode.ALREADY_CHECKED_OUT);
        }
    }

    private boolean hasCheckedIn(List<WorkSchedule> schedules) {
        List<WorkAttendance> attendances = workAttendanceRepository.findAllByScheduleIn(schedules);

        return attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);
    }

    private List<WorkSchedule> findConsecutiveGroup(
            List<WorkSchedule> schedules,
            WorkSchedule targetSchedule
    ) {
        List<WorkSchedule> sorted = new ArrayList<>(schedules);
        sorted.sort(Comparator.comparing(WorkSchedule::getStartTime));

        int targetIndex = sorted.indexOf(targetSchedule);
        int startIndex = targetIndex;
        int endIndex = targetIndex;
        while (startIndex > 0
                && sorted.get(startIndex - 1).getEndTime().equals(sorted.get(startIndex).getStartTime())) {
            startIndex--;
        }
        while (endIndex < sorted.size() - 1
                && sorted.get(endIndex).getEndTime().equals(sorted.get(endIndex + 1).getStartTime())) {
            endIndex++;
        }
        return new ArrayList<>(sorted.subList(startIndex, endIndex + 1));
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

    private LocalDateTime toDateTime(WorkSchedule schedule, LocalTime time) {
        return LocalDateTime.of(schedule.getDate(), time);
    }
}
