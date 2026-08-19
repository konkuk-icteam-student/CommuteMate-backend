package com.better.CommuteMate.domain.schedule.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkScheduleSettingTest {

    @Test
    @DisplayName("isApplyPeriod - applyEndAt이 종료일 00:00으로 저장돼도 종료일 당일 낮에는 true다")
    void isApplyPeriod_EndDateStoredAtMidnight_TrueOnEndDate() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = settingOf(today.minusDays(9), today.atStartOfDay());

        boolean result = setting.isApplyPeriod(today.atTime(14, 0));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isApplyPeriod - applyEndAt이 종료일 23:59:59로 저장돼도 종료일 당일 낮에는 true다")
    void isApplyPeriod_EndDateStoredAtEndOfDay_TrueOnEndDate() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = settingOf(today.minusDays(9), today.atTime(23, 59, 59));

        boolean result = setting.isApplyPeriod(today.atTime(14, 0));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isApplyPeriod - 종료일 다음날에는 false다")
    void isApplyPeriod_DayAfterEndDate_False() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = settingOf(today.minusDays(10), today.minusDays(1).atStartOfDay());

        boolean result = setting.isApplyPeriod(today.atTime(9, 0));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isApplyPeriod - 시작일 당일 00:00 직후에는 true다")
    void isApplyPeriod_OnStartDateJustAfterMidnight_True() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = settingOf(today, today.plusDays(9).atStartOfDay());

        boolean result = setting.isApplyPeriod(today.atTime(0, 0, 1));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isApplyPeriod - 시작일 전날에는 false다")
    void isApplyPeriod_DayBeforeStartDate_False() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = settingOf(today.plusDays(1), today.plusDays(10).atStartOfDay());

        boolean result = setting.isApplyPeriod(today.atTime(23, 59));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isApplyPeriod - applyEnabled=false면 기간 내여도 false다")
    void isApplyPeriod_ApplyDisabled_FalseEvenWithinPeriod() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(today.getYear()).month(today.getMonthValue())
                .applyStartAt(today.minusDays(3).atStartOfDay())
                .applyEndAt(today.plusDays(3).atStartOfDay())
                .applyEnabled(false)
                .maxConcurrentWorkers(1)
                .monthlyRequiredMinutes(60)
                .build();

        boolean result = setting.isApplyPeriod(today.atTime(12, 0));

        assertThat(result).isFalse();
    }

    private WorkScheduleSetting settingOf(LocalDate startDate, LocalDateTime applyEndAt) {
        return WorkScheduleSetting.builder()
                .organizationId(10L).year(startDate.getYear()).month(startDate.getMonthValue())
                .applyStartAt(startDate.atStartOfDay())
                .applyEndAt(applyEndAt)
                .maxConcurrentWorkers(1)
                .monthlyRequiredMinutes(60)
                .build();
    }
}
