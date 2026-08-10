package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public class ScheduleSettingResponse extends ResponseDetail {
    public final int year;
    public final int month;
    public final boolean isConfigured;
    public final boolean applyStarted;
    public final LocalDate applyStartDate;
    public final LocalDate applyEndDate;
    public final Integer maxConcurrentWorkers;
    public final Integer minWorkUnitMinutes;
    public final Integer weeklyMinMinutes;
    public final Integer weeklyMaxMinutes;
    public final Integer monthlyMinMinutes;
    public final Integer monthlyMaxMinutes;
    public final List<LocalDate> unavailableDates;
    public final List<UnavailableTimeRange> unavailableTimeRanges;

    private ScheduleSettingResponse(
            int year,
            int month,
            boolean isConfigured,
            boolean applyStarted,
            LocalDate applyStartDate,
            LocalDate applyEndDate,
            Integer maxConcurrentWorkers,
            Integer minWorkUnitMinutes,
            Integer weeklyMinMinutes,
            Integer weeklyMaxMinutes,
            Integer monthlyMinMinutes,
            Integer monthlyMaxMinutes,
            List<LocalDate> unavailableDates,
            List<UnavailableTimeRange> unavailableTimeRanges
    ) {
        this.year = year;
        this.month = month;
        this.isConfigured = isConfigured;
        this.applyStarted = applyStarted;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.minWorkUnitMinutes = minWorkUnitMinutes;
        this.weeklyMinMinutes = weeklyMinMinutes;
        this.weeklyMaxMinutes = weeklyMaxMinutes;
        this.monthlyMinMinutes = monthlyMinMinutes;
        this.monthlyMaxMinutes = monthlyMaxMinutes;
        this.unavailableDates = unavailableDates;
        this.unavailableTimeRanges = unavailableTimeRanges;
    }

    public static ScheduleSettingResponse notConfigured(int year, int month) {
        return new ScheduleSettingResponse(
                year, month, false, false,
                null, null, null, null, null, null, null, null,
                List.of(), List.of()
        );
    }

    public static ScheduleSettingResponse configured(
            WorkScheduleSetting setting,
            List<WorkUnavailableTime> unavailableTimes,
            LocalDateTime now
    ) {
        List<LocalDate> unavailableDates = unavailableTimes.stream()
                .filter(ScheduleSettingResponse::isFullDay)
                .map(WorkUnavailableTime::getDate)
                .distinct()
                .sorted()
                .toList();
        List<UnavailableTimeRange> unavailableTimeRanges = unavailableTimes.stream()
                .filter(time -> !isFullDay(time))
                .map(time -> new UnavailableTimeRange(time.getStartTime(), time.getEndTime()))
                .distinct()
                .sorted(Comparator.comparing(UnavailableTimeRange::start)
                        .thenComparing(UnavailableTimeRange::end))
                .toList();

        return new ScheduleSettingResponse(
                setting.getYear(),
                setting.getMonth(),
                true,
                Boolean.TRUE.equals(setting.getApplyEnabled())
                        && !now.isBefore(setting.getApplyStartAt()),
                setting.getApplyStartAt().toLocalDate(),
                setting.getApplyEndAt().toLocalDate(),
                setting.getMaxConcurrentWorkers(),
                setting.getMinWorkUnitMinutes(),
                setting.getWeeklyMinMinutes(),
                setting.getWeeklyMaxMinutes(),
                setting.getMonthlyMinMinutes(),
                setting.getMonthlyMaxMinutes(),
                unavailableDates,
                unavailableTimeRanges
        );
    }

    private static boolean isFullDay(WorkUnavailableTime time) {
        if (!LocalTime.MIN.equals(time.getStartTime())) return false;
        // sentinel: start=00:00, end=00:00 (현재 DB 저장 방식)
        if (LocalTime.MIN.equals(time.getEndTime())) return true;
        // 이전 호환: end=23:59:59... (LocalTime.MAX 기반, PostgreSQL 나노초 반올림으로 초 단위 비교)
        return time.getEndTime().toSecondOfDay() == LocalTime.MAX.toSecondOfDay();
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record UnavailableTimeRange(LocalTime start, LocalTime end) {
    }
}
