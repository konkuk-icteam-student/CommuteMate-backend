package com.better.CommuteMate.schedule.application.dtos;

import java.time.LocalDateTime;
import java.time.YearMonth;

public record MonthlyScheduleConfigCommand(
        Integer scheduleYear,
        Integer scheduleMonth,
        Integer maxConcurrent,
        LocalDateTime applyStartTime,
        LocalDateTime applyEndTime,
        Long userId) {

    public static MonthlyScheduleConfigCommand from(
            Integer scheduleYear,
            Integer scheduleMonth,
            Integer maxConcurrent,
            Long userId) {
        // 기본 신청 기간 설정: 해당 월의 전달 23일 00:00 ~ 27일 00:00
        LocalDateTime applyStartTime = getDefaultApplyStartTime(scheduleYear, scheduleMonth);
        LocalDateTime applyEndTime = getDefaultApplyEndTime(scheduleYear, scheduleMonth);

        return new MonthlyScheduleConfigCommand(
                scheduleYear,
                scheduleMonth,
                maxConcurrent,
                applyStartTime,
                applyEndTime,
                userId);
    }

    /**
     * 신청 기간의 기본값 시작 시간 계산
     * 규칙: 해당 월의 전달 23일 00:00 (자정)
     * 예: 2025년 12월 제한 → 2025년 11월 23일 00:00
     */
    private static LocalDateTime getDefaultApplyStartTime(Integer scheduleYear, Integer scheduleMonth) {
        YearMonth targetMonth = YearMonth.of(scheduleYear, scheduleMonth);
        YearMonth previousMonth = targetMonth.minusMonths(1);
        return previousMonth.atDay(23).atStartOfDay();
    }

    /**
     * 신청 기간의 기본값 종료 시간 계산
     * 규칙: 해당 월의 전달 27일 00:00 (자정)
     * 예: 2025년 12월 제한 → 2025년 11월 27일 00:00
     */
    private static LocalDateTime getDefaultApplyEndTime(Integer scheduleYear, Integer scheduleMonth) {
        YearMonth targetMonth = YearMonth.of(scheduleYear, scheduleMonth);
        YearMonth previousMonth = targetMonth.minusMonths(1);
        return previousMonth.atDay(27).atStartOfDay();
    }
}
