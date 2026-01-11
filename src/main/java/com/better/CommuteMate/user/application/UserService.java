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

    /**
     * 사용자의 기본 정보를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 응답 (ID, 이메일, 이름, 역할, 조직 ID)
     * @throws BasicException 사용자가 존재하지 않을 경우 USER_NOT_FOUND 에러 발생
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Integer userId) {
        // 사용자 조회 (없으면 예외 발생)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));
        return new UserInfoResponse(user);
    }

    /**
     * 특정 사용자의 주간 근무 시간을 계산합니다.
     * 이번 주(월~일)의 총 근무 시간을 분 단위로 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 주간 총 근무 시간 (분)
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getWeeklyWorkTime(Integer userId) {
        LocalDate now = LocalDate.now();
        // 이번 주 월요일 00:00 계산
        LocalDate startOfWeek = now.with(WeekFields.ISO.dayOfWeek(), 1);
        LocalDateTime start = startOfWeek.atStartOfDay();
        // 다음 주 월요일 00:00 (이번 주 일요일 24:00) 계산
        LocalDateTime end = startOfWeek.plusDays(7).atStartOfDay();

        long minutes = calculateTotalWorkTime(userId, start, end);
        return new UserWorkTimeResponse(minutes, "WEEKLY");
    }

    /**
     * 특정 사용자의 월간 근무 시간을 계산합니다.
     * 이번 달(1일~말일)의 총 근무 시간을 분 단위로 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 월간 총 근무 시간 (분)
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getMonthlyWorkTime(Integer userId) {
        LocalDate now = LocalDate.now();
        // 이번 달 1일 00:00 계산
        LocalDate startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime start = startOfMonth.atStartOfDay();
        // 다음 달 1일 00:00 (이번 달 말일 24:00) 계산
        LocalDateTime end = startOfMonth.plusMonths(1).atStartOfDay();

        long minutes = calculateTotalWorkTime(userId, start, end);
        return new UserWorkTimeResponse(minutes, "MONTHLY");
    }

    /**
     * 주어진 기간 동안의 총 근무 시간을 계산하는 내부 메서드입니다.
     * 승인된 일정(WS02)에 대해서만 실제 출퇴근 기록을 바탕으로 시간을 산출합니다.
     *
     * @param userId 사용자 ID
     * @param start 조회 시작 일시
     * @param end 조회 종료 일시
     * @return 총 근무 시간 (분)
     */
    private long calculateTotalWorkTime(Integer userId, LocalDateTime start, LocalDateTime end) {
        // 해당 기간 내의 모든 일정 조회
        List<WorkSchedule> schedules = workSchedulesRepository.findAllSchedulesByUserAndDateRange(
                userId, start, end);
        
        long totalMinutes = 0;
        for (WorkSchedule schedule : schedules) {
            // 승인된 일정(WS02)만 계산에 포함
            if (schedule.getStatusCode() != CodeType.WS02) {
                continue;
            }

            // 해당 일정의 출퇴근 기록 조회 및 시간 계산
            List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
            totalMinutes += calculateDuration(schedule, attendances);
        }
        return totalMinutes;
    }

    /**
     * 단일 일정에 대한 실제 근무 시간을 계산합니다.
     * - 출근 기록이 없으면 0분 처리
     * - 퇴근 기록이 없으면 현재 시간을 기준으로 계산 (진행 중인 경우)
     * - 예정된 스케줄 시간을 벗어난 출퇴근은 스케줄 시간으로 보정 (초과 근무 제외)
     *
     * @param schedule 근무 일정
     * @param attendances 해당 일정의 출퇴근 기록 리스트
     * @return 근무 시간 (분)
     */
    private long calculateDuration(WorkSchedule schedule, List<WorkAttendance> attendances) {
        // 출근 기록(CT01) 조회
        Optional<LocalDateTime> checkIn = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        // 퇴근 기록(CT02) 조회
        Optional<LocalDateTime> checkOut = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        // 출근 기록이 없으면 0분 반환
        if (checkIn.isEmpty()) {
            return 0;
        }

        LocalDateTime start = checkIn.get();
        // 퇴근 기록이 없으면 현재 시간 기준 (근무 중)
        LocalDateTime end = checkOut.orElse(LocalDateTime.now());

        // 실제 출퇴근 시간이 스케줄 예정 시간을 벗어나면 스케줄 시간으로 보정 (초과 근무 제외)
        if (start.isBefore(schedule.getStartTime())) start = schedule.getStartTime();
        if (end.isAfter(schedule.getEndTime())) end = schedule.getEndTime();

        // 시작 시간이 종료 시간보다 늦으면 0분 처리 (유효하지 않은 경우)
        if (start.isAfter(end)) return 0;

        return Duration.between(start, end).toMinutes();
    }
}
