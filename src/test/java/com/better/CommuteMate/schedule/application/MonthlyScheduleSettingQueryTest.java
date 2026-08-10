package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduleSettingQueryTest {

    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkUnavailableTimeRepository unavailableTimeRepository;

    private MonthlyScheduleSettingService service;

    @BeforeEach
    void setUp() {
        service = new MonthlyScheduleSettingService(
                settingRepository, scheduleRepository, unavailableTimeRepository
        );
    }

    @Test
    void returnsConfiguredSettingWithDistinctUnavailableValues() {
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .settingId(1L)
                .organizationId(10L)
                .year(2026)
                .month(4)
                .applyStartAt(LocalDateTime.of(2000, 4, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2026, 4, 10, 23, 59))
                .applyEnabled(true)
                .maxConcurrentWorkers(4)
                .minWorkUnitMinutes(120)
                .weeklyMinMinutes(300)
                .weeklyMaxMinutes(540)
                .monthlyMinMinutes(1200)
                .monthlyMaxMinutes(1620)
                .monthlyRequiredMinutes(1620)
                .build();
        WorkUnavailableTime fullDay = unavailable(
                setting, LocalDate.of(2026, 4, 19), LocalTime.MIN, LocalTime.MAX
        );
        WorkUnavailableTime firstRange = unavailable(
                setting, LocalDate.of(2026, 4, 1), LocalTime.of(11, 0), LocalTime.of(13, 0)
        );
        WorkUnavailableTime duplicatedRange = unavailable(
                setting, LocalDate.of(2026, 4, 2), LocalTime.of(11, 0), LocalTime.of(13, 0)
        );
        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(unavailableTimeRepository.findBySettingAndDateBetween(
                setting, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(fullDay, firstRange, duplicatedRange));

        var response = service.get(10L, 2026, 4);

        assertThat(response.isConfigured).isTrue();
        assertThat(response.applyStarted).isTrue();
        assertThat(response.unavailableDates).containsExactly(LocalDate.of(2026, 4, 19));
        assertThat(response.unavailableTimeRanges).singleElement().satisfies(range -> {
            assertThat(range.start()).isEqualTo(LocalTime.of(11, 0));
            assertThat(range.end()).isEqualTo(LocalTime.of(13, 0));
        });
    }

    @Test
    void returnsNullSettingsWhenMonthIsNotConfigured() {
        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.empty());

        var response = service.get(10L, 2026, 4);

        assertThat(response.isConfigured).isFalse();
        assertThat(response.applyStarted).isFalse();
        assertThat(response.applyStartDate).isNull();
        assertThat(response.maxConcurrentWorkers).isNull();
        assertThat(response.unavailableDates).isEmpty();
        assertThat(response.unavailableTimeRanges).isEmpty();
    }

    @Test
    void rejectsInvalidYearOrMonth() {
        assertThatThrownBy(() -> service.get(10L, 2026, 13))
                .isInstanceOf(CustomException.class);
    }

    private WorkUnavailableTime unavailable(
            WorkScheduleSetting setting,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
        return WorkUnavailableTime.builder()
                .setting(setting)
                .date(date)
                .startTime(start)
                .endTime(end)
                .build();
    }
}
