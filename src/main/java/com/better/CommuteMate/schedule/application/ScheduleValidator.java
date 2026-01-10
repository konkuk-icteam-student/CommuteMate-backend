package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final MonthlyScheduleConfigRepository monthlyScheduleConfigRepository;

    @Value("${app.schedule.concurrent.max}")
    private int DEFAULT_MAX_CONCURRENT_SCHEDULES;

    // 근무 시간 관련 상수 (분 단위)
    private static final long MAX_MONTHLY_MINUTES = 27 * 60; // 월 최대 27시간
    private static final long MAX_WEEKLY_MINUTES = 13 * 60;  // 주 최대 13시간
    private static final long MIN_SESSION_MINUTES = 2 * 60;  // 1회 최소 2시간

    // 입력 일정의 분 단위가 00분, 30분 만 존재한다고 가정
    public boolean isScheduleInsertable(WorkScheduleCommand slot) {
        LocalDate date = slot.start().toLocalDate();  // 찾고 싶은 날짜
        LocalDateTime startOfDay = date.atStartOfDay();  // 찾고 싶은 날짜
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();   // 찾고 싶은 날짜 + 1일

        // 월별 최대 동시 스케줄 수 조회
        int maxConcurrentSchedules = getMaxConcurrentSchedules(date.getYear(), date.getMonthValue());

        List<WorkSchedule> daySchedules = workSchedulesRepository.findByDate(startOfDay,endOfDay);

        LocalTime startTime = slot.start().toLocalTime();
        LocalTime endTime = slot.end().toLocalTime();

        LocalTime currentCheckPoint = startTime.plusMinutes(15);
        // isDeleted가 false인 스케줄들 중에서, 15분 단위로 겹치는 스케줄이 최대 동시 스케줄 수를 초과하는지 확인
        while (currentCheckPoint.isBefore(endTime)) {
            LocalTime finalCheckPoint = currentCheckPoint;

            long overlappingCount = daySchedules.stream()
                .filter(schedule -> !schedule.getIsDeleted()) // 삭제된 스케줄 제외
                .filter(schedule ->
                    schedule.getStartTime().toLocalTime().isBefore(finalCheckPoint) && // 15분 시점이 스케줄 범위에 포함되는지
                    schedule.getEndTime().toLocalTime().isAfter(finalCheckPoint))
                .count();

            if (overlappingCount >= maxConcurrentSchedules) {
                return false;
            }

            currentCheckPoint = currentCheckPoint.plusMinutes(30);
        }
        return true;
    }

    private int getMaxConcurrentSchedules(int scheduleYear, int scheduleMonth) {
        return monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(scheduleYear, scheduleMonth)
                .map(MonthlyScheduleConfig::getMaxConcurrent)
                .orElse(DEFAULT_MAX_CONCURRENT_SCHEDULES); // 존재하지 않을 경우, 기본값 반환
    }

    /**
     * 1회 최소 근무 시간(2시간) 검증
     */
    public void validateMinWorkTime(WorkScheduleCommand slot) {
        long minutes = java.time.Duration.between(slot.start(), slot.end()).toMinutes();
        if (minutes < MIN_SESSION_MINUTES) {
            throw com.better.CommuteMate.global.exceptions.BasicException.of(
                com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode.MIN_WORK_TIME_NOT_MET
            );
        }
    }

    /**
     * 월 총 근무 시간(27시간) 초과 여부 검증
     */
    public void validateTotalWorkTime(long currentMinutes, long newMinutes) {
        if (currentMinutes + newMinutes > MAX_MONTHLY_MINUTES) {
            throw com.better.CommuteMate.global.exceptions.BasicException.of(
                com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode.TOTAL_WORK_TIME_EXCEEDED
            );
        }
    }

    /**
     * 주 최대 근무 시간(13시간) 초과 여부 검증
     */
    public void validateWeeklyWorkTime(long currentMinutes, long newMinutes) {
        if (currentMinutes + newMinutes > MAX_WEEKLY_MINUTES) {
            throw com.better.CommuteMate.global.exceptions.BasicException.of(
                com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode.WEEKLY_WORK_TIME_EXCEEDED
            );
        }
    }
}