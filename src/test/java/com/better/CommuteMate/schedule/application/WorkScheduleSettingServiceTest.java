package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkScheduleSettingServiceTest {

    @Mock
    private WorkScheduleSettingRepository workScheduleSettingRepository;

    @InjectMocks
    private WorkScheduleSettingService workScheduleSettingService;

    @Test
    @DisplayName("setApplyTerm - 신규 설정 저장 시 applyStartAt은 시작일 00:00, applyEndAt은 종료일 23:59:59.999999999로 정규화된다")
    void setApplyTerm_NewSetting_NormalizesToStartOfDayAndEndOfDay() throws Exception {
        setDefaultMaxConcurrent(workScheduleSettingService, 4);
        when(workScheduleSettingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 8))
                .thenReturn(Optional.empty());
        when(workScheduleSettingRepository.save(any(WorkScheduleSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 원본 입력 시각이 자정이 아니어도(예: 09:30, 18:45) 날짜만 취해 정규화되어야 한다.
        LocalDateTime rawStart = LocalDate.of(2026, 8, 1).atTime(9, 30);
        LocalDateTime rawEnd = LocalDate.of(2026, 8, 10).atTime(18, 45);

        WorkScheduleSetting result = workScheduleSettingService.setApplyTerm(
                10L, 2026, 8, rawStart, rawEnd, "admin1");

        assertThat(result.getApplyStartAt()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay());
        assertThat(result.getApplyEndAt()).isEqualTo(LocalDate.of(2026, 8, 10).atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("setApplyTerm - 기존 설정 갱신 시에도 동일하게 시작일 00:00 / 종료일 23:59:59.999999999로 정규화된다")
    void setApplyTerm_ExistingSetting_NormalizesToStartOfDayAndEndOfDay() {
        WorkScheduleSetting existing = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2020, 1, 2, 0, 0))
                .maxConcurrentWorkers(1)
                .monthlyRequiredMinutes(60)
                .build();
        when(workScheduleSettingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 8))
                .thenReturn(Optional.of(existing));

        LocalDateTime rawStart = LocalDate.of(2026, 8, 1).atTime(23, 0);
        LocalDateTime rawEnd = LocalDate.of(2026, 8, 10).atTime(0, 5);

        WorkScheduleSetting result = workScheduleSettingService.setApplyTerm(
                10L, 2026, 8, rawStart, rawEnd, "admin1");

        assertThat(result.getApplyStartAt()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay());
        assertThat(result.getApplyEndAt()).isEqualTo(LocalDate.of(2026, 8, 10).atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("setApplyTerm - 정규화 후 시작 날짜가 종료 날짜보다 늦으면 INVALID_APPLY_TERM")
    void setApplyTerm_StartDateAfterEndDate_ThrowsInvalidApplyTerm() {
        LocalDateTime rawStart = LocalDate.of(2026, 8, 11).atStartOfDay();
        LocalDateTime rawEnd = LocalDate.of(2026, 8, 10).atTime(23, 0);

        assertThatThrownBy(() -> workScheduleSettingService.setApplyTerm(
                10L, 2026, 8, rawStart, rawEnd, "admin1"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("setApplyTerm - 정규화 후에도 isApplyPeriod 판정 결과는 종료일 당일 true다 (통일 전후 판정 동일성)")
    void setApplyTerm_NormalizedSetting_IsApplyPeriodTrueOnEndDate() {
        WorkScheduleSetting existing = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2020, 1, 2, 0, 0))
                .maxConcurrentWorkers(1)
                .monthlyRequiredMinutes(60)
                .build();
        when(workScheduleSettingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 8))
                .thenReturn(Optional.of(existing));

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        WorkScheduleSetting result = workScheduleSettingService.setApplyTerm(
                10L, 2026, 8, start.atStartOfDay(), end.atStartOfDay(), "admin1");

        assertThat(result.isApplyPeriod(end.atTime(14, 0))).isTrue();
    }

    @Test
    @DisplayName("getDefaultApplyStartAt - 대상 월 전달 23일 00:00을 반환한다")
    void getDefaultApplyStartAt_ReturnsPreviousMonth23rdMidnight() {
        LocalDateTime result = workScheduleSettingService.getDefaultApplyStartAt(2026, 8);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 7, 23, 0, 0));
    }

    @Test
    @DisplayName("getDefaultApplyEndAt - 대상 월 전달 27일 23:59:59.999999999를 반환한다")
    void getDefaultApplyEndAt_ReturnsPreviousMonth27thEndOfDay() {
        LocalDateTime result = workScheduleSettingService.getDefaultApplyEndAt(2026, 8);

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 27).atTime(LocalTime.MAX));
    }

    private void setDefaultMaxConcurrent(WorkScheduleSettingService service, int value) throws Exception {
        Field field = WorkScheduleSettingService.class.getDeclaredField("DEFAULT_MAX_CONCURRENT_WORKERS");
        field.setAccessible(true);
        field.setInt(service, value);
    }
}
