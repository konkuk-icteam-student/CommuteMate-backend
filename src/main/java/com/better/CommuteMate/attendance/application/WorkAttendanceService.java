package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.attendance.application.exception.AttendanceErrorCode;
import com.better.CommuteMate.attendance.application.exception.AttendanceException;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkAttendanceService {

    private final WorkAttendanceRepository workAttendanceRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final UserRepository userRepository;
    private final QrTokenManager qrTokenManager;

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
            throw new AttendanceException(AttendanceErrorCode.INVALID_QR_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<WorkSchedule> schedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, startOfDay, endOfDay);

        if (schedules.isEmpty()) {
            throw new AttendanceException(AttendanceErrorCode.NO_SCHEDULE_FOUND);
        }

        WorkSchedule targetSchedule = findTargetScheduleForCheckIn(schedules, now);

        if (targetSchedule == null) {
            throw new AttendanceException(AttendanceErrorCode.NOT_WORK_TIME);
        }

        checkIfAlreadyCheckedIn(targetSchedule);

        WorkAttendance attendance = WorkAttendance.builder()
                .user(user)
                .schedule(targetSchedule)
                .checkTime(now)
                .checkTypeCode(CodeType.CT01)
                .verified(true)
                .build();

        workAttendanceRepository.save(attendance);
    }

    /**
     * 사용자용: 퇴근 체크
     */
    @Transactional
    public void checkOut(Long userId, String qrToken) {
        if (!qrTokenManager.validateToken(qrToken)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_QR_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<WorkSchedule> schedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                userId, startOfDay, endOfDay);

        if (schedules.isEmpty()) {
            throw new AttendanceException(AttendanceErrorCode.NO_SCHEDULE_FOUND);
        }

        WorkSchedule targetSchedule = findTargetScheduleForCheckOut(schedules, now);

        if (targetSchedule == null) {
            throw new AttendanceException(AttendanceErrorCode.NOT_WORK_TIME);
        }

        if (!hasCheckedIn(targetSchedule)) {
            throw new AttendanceException(AttendanceErrorCode.CHECK_IN_REQUIRED);
        }
        
        checkIfAlreadyCheckedOut(targetSchedule);

        WorkAttendance attendance = WorkAttendance.builder()
                .user(user)
                .schedule(targetSchedule)
                .checkTime(now)
                .checkTypeCode(CodeType.CT02)
                .verified(true)
                .build();

        workAttendanceRepository.save(attendance);
    }

    /**
     * 특정 날짜의 출퇴근 이력 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceHistoryResponse> getAttendanceHistory(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<WorkAttendance> attendances = workAttendanceRepository.findByUser_UserIdAndCheckTimeBetween(userId, start, end);

        return attendances.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    private WorkSchedule findTargetScheduleForCheckIn(List<WorkSchedule> schedules, LocalDateTime now) {
        for (WorkSchedule schedule : schedules) {
            LocalDateTime start = schedule.getStartTime();
            LocalDateTime end = schedule.getEndTime();

            if (now.isAfter(start.minusMinutes(10)) && now.isBefore(end)) {
                return schedule;
            }
        }
        return null;
    }

    private WorkSchedule findTargetScheduleForCheckOut(List<WorkSchedule> schedules, LocalDateTime now) {
        for (WorkSchedule schedule : schedules) {
            LocalDateTime end = schedule.getEndTime();

            if (now.isAfter(end.minusMinutes(5)) && now.isBefore(end.plusHours(1))) {
                return schedule;
            }
        }
        return null;
    }

    private void checkIfAlreadyCheckedIn(WorkSchedule schedule) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        boolean exists = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);
        if (exists) {
            throw new AttendanceException(AttendanceErrorCode.ALREADY_CHECKED_IN);
        }
    }
    
    private void checkIfAlreadyCheckedOut(WorkSchedule schedule) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        boolean exists = attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT02);
        if (exists) {
            throw new AttendanceException(AttendanceErrorCode.ALREADY_CHECKED_OUT);
        }
    }

    private boolean hasCheckedIn(WorkSchedule schedule) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        return attendances.stream()
                .anyMatch(a -> a.getCheckTypeCode() == CodeType.CT01);
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
}
