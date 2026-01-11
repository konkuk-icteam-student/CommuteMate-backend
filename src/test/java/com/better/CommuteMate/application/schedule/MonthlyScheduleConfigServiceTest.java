package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleConfigCommand;
import com.better.CommuteMate.schedule.application.dtos.SetApplyTermCommand;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleConfigException;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduleConfigServiceTest {

    @Mock
    private MonthlyScheduleConfigRepository monthlyScheduleConfigRepository;

    @InjectMocks
    private MonthlyScheduleConfigService monthlyScheduleConfigService;

    @BeforeEach
    void setUp() {
        // DEFAULT_MAX_CONCURRENT_SCHEDULES 기본값 설정
        ReflectionTestUtils.setField(monthlyScheduleConfigService, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 10);
    }

    // ========== setMonthlyLimit 테스트 ==========

    @Test
    @DisplayName("월별 제한 설정 - 신규 생성 성공")
    void setMonthlyLimit_NewConfig_Success() {
        // Given
        MonthlyScheduleConfigCommand command = MonthlyScheduleConfigCommand.from(
                2025, 11, 15, 1
        );

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                .thenReturn(Optional.empty());

        MonthlyScheduleConfig savedConfig = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(15)
                .applyStartTime(command.applyStartTime())
                .applyEndTime(command.applyEndTime())
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigRepository.save(any(MonthlyScheduleConfig.class)))
                .thenReturn(savedConfig);

        // When
        MonthlyScheduleConfig result = monthlyScheduleConfigService.setMonthlyLimit(command);

        // Then
        assertThat(result.getScheduleYear()).isEqualTo(2025);
        assertThat(result.getScheduleMonth()).isEqualTo(11);
        assertThat(result.getMaxConcurrent()).isEqualTo(15);
        verify(monthlyScheduleConfigRepository, times(1)).save(any(MonthlyScheduleConfig.class));
    }

    @Test
    @DisplayName("월별 제한 설정 - 기존 설정 업데이트 성공")
    void setMonthlyLimit_ExistingConfig_UpdateSuccess() {
        // Given
        MonthlyScheduleConfig existingConfig = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(10)
                .applyStartTime(LocalDateTime.of(2025, 10, 23, 0, 0))
                .applyEndTime(LocalDateTime.of(2025, 10, 27, 0, 0))
                .createdBy(1)
                .updatedBy(1)
                .build();

        MonthlyScheduleConfigCommand command = MonthlyScheduleConfigCommand.from(
                2025, 11, 20, 1
        );

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                .thenReturn(Optional.of(existingConfig));

        when(monthlyScheduleConfigRepository.save(any(MonthlyScheduleConfig.class)))
                .thenReturn(existingConfig);

        // When
        MonthlyScheduleConfig result = monthlyScheduleConfigService.setMonthlyLimit(command);

        // Then
        assertThat(result.getMaxConcurrent()).isEqualTo(20);
        verify(monthlyScheduleConfigRepository, times(1)).save(existingConfig);
    }

    // ========== getMonthlyLimit 테스트 ==========

    @Test
    @DisplayName("월별 제한 조회 - 존재하는 경우")
    void getMonthlyLimit_Exists_Success() {
        // Given
        MonthlyScheduleConfig config = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(10)
                .applyStartTime(LocalDateTime.of(2025, 10, 23, 0, 0))
                .applyEndTime(LocalDateTime.of(2025, 10, 27, 0, 0))
                .build();

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                .thenReturn(Optional.of(config));

        // When
        Optional<MonthlyScheduleConfig> result = monthlyScheduleConfigService.getMonthlyLimit(2025, 11);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getScheduleYear()).isEqualTo(2025);
        assertThat(result.get().getScheduleMonth()).isEqualTo(11);
    }

    @Test
    @DisplayName("월별 제한 조회 - 존재하지 않는 경우")
    void getMonthlyLimit_NotExists_ReturnsEmpty() {
        // Given
        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 12))
                .thenReturn(Optional.empty());

        // When
        Optional<MonthlyScheduleConfig> result = monthlyScheduleConfigService.getMonthlyLimit(2025, 12);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getAllMonthlyLimits 테스트 ==========

    @Test
    @DisplayName("모든 월별 제한 조회 - 여러 개 존재하는 경우")
    void getAllMonthlyLimits_MultipleConfigs_Success() {
        // Given
        MonthlyScheduleConfig config1 = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(10)
                .applyStartTime(LocalDateTime.of(2025, 10, 23, 0, 0))
                .applyEndTime(LocalDateTime.of(2025, 10, 27, 0, 0))
                .build();

        MonthlyScheduleConfig config2 = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(12)
                .maxConcurrent(15)
                .applyStartTime(LocalDateTime.of(2025, 11, 23, 0, 0))
                .applyEndTime(LocalDateTime.of(2025, 11, 27, 0, 0))
                .build();

        when(monthlyScheduleConfigRepository.findAll())
                .thenReturn(List.of(config1, config2));

        // When
        List<MonthlyScheduleConfig> result = monthlyScheduleConfigService.getAllMonthlyLimits();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getScheduleMonth()).isEqualTo(11);
        assertThat(result.get(1).getScheduleMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("모든 월별 제한 조회 - 비어있는 경우")
    void getAllMonthlyLimits_Empty_ReturnsEmptyList() {
        // Given
        when(monthlyScheduleConfigRepository.findAll())
                .thenReturn(List.of());

        // When
        List<MonthlyScheduleConfig> result = monthlyScheduleConfigService.getAllMonthlyLimits();

        // Then
        assertThat(result).isEmpty();
    }

    // ========== isCurrentlyInApplyTerm 테스트 ==========

    @Test
    @DisplayName("신청 기간 확인 - 설정이 없는 경우 false 반환")
    void isCurrentlyInApplyTerm_NoConfig_ReturnsFalse() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 1, 9, 0);

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                .thenReturn(Optional.empty());

        // When
        boolean result = monthlyScheduleConfigService.isCurrentlyInApplyTerm(startTime);

        // Then
        assertThat(result).isFalse();
    }

    // ========== setApplyTerm 테스트 ==========

    @Test
    @DisplayName("신청 기간 설정 - 신규 생성 성공")
    void setApplyTerm_NewConfig_Success() {
        // Given
        SetApplyTermCommand command = new SetApplyTermCommand(
                2025, 12,
                LocalDateTime.of(2025, 11, 23, 0, 0),
                LocalDateTime.of(2025, 11, 27, 0, 0),
                1
        );

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 12))
                .thenReturn(Optional.empty());

        MonthlyScheduleConfig savedConfig = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(12)
                .applyStartTime(command.applyStartTime())
                .applyEndTime(command.applyEndTime())
                .maxConcurrent(10) // DEFAULT_MAX_CONCURRENT_SCHEDULES
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigRepository.save(any(MonthlyScheduleConfig.class)))
                .thenReturn(savedConfig);

        // When
        MonthlyScheduleConfig result = monthlyScheduleConfigService.setApplyTerm(command);

        // Then
        assertThat(result.getScheduleYear()).isEqualTo(2025);
        assertThat(result.getScheduleMonth()).isEqualTo(12);
        assertThat(result.getApplyStartTime()).isEqualTo(command.applyStartTime());
        assertThat(result.getApplyEndTime()).isEqualTo(command.applyEndTime());
        assertThat(result.getMaxConcurrent()).isEqualTo(10); // 기본값 확인
        verify(monthlyScheduleConfigRepository, times(1)).save(any(MonthlyScheduleConfig.class));
    }

    @Test
    @DisplayName("신청 기간 설정 - 기존 설정 업데이트 성공")
    void setApplyTerm_ExistingConfig_UpdateSuccess() {
        // Given
        MonthlyScheduleConfig existingConfig = MonthlyScheduleConfig.builder()
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(15)
                .applyStartTime(LocalDateTime.of(2025, 10, 20, 0, 0))
                .applyEndTime(LocalDateTime.of(2025, 10, 25, 0, 0))
                .createdBy(1)
                .updatedBy(1)
                .build();

        SetApplyTermCommand command = new SetApplyTermCommand(
                2025, 11,
                LocalDateTime.of(2025, 10, 23, 0, 0),
                LocalDateTime.of(2025, 10, 27, 0, 0),
                1
        );

        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                .thenReturn(Optional.of(existingConfig));

        when(monthlyScheduleConfigRepository.save(any(MonthlyScheduleConfig.class)))
                .thenReturn(existingConfig);

        // When
        MonthlyScheduleConfig result = monthlyScheduleConfigService.setApplyTerm(command);

        // Then
        assertThat(result.getApplyStartTime()).isEqualTo(command.applyStartTime());
        assertThat(result.getApplyEndTime()).isEqualTo(command.applyEndTime());
        verify(monthlyScheduleConfigRepository, times(1)).save(existingConfig);
    }

    @Test
    @DisplayName("신청 기간 설정 - 시작 시간이 종료 시간보다 늦은 경우 실패")
    void setApplyTerm_StartTimeAfterEndTime_ThrowsException() {
        // Given
        SetApplyTermCommand command = new SetApplyTermCommand(
                2025, 11,
                LocalDateTime.of(2025, 10, 27, 0, 0), // 종료 시간
                LocalDateTime.of(2025, 10, 23, 0, 0), // 시작 시간
                1
        );

        // When & Then
        assertThatThrownBy(() -> monthlyScheduleConfigService.setApplyTerm(command))
                .isInstanceOf(ScheduleConfigException.class);

        verify(monthlyScheduleConfigRepository, never()).save(any(MonthlyScheduleConfig.class));
    }

    @Test
    @DisplayName("신청 기간 설정 - 시작 시간과 종료 시간이 같은 경우 실패")
    void setApplyTerm_StartTimeEqualsEndTime_ThrowsException() {
        // Given
        LocalDateTime sameTime = LocalDateTime.of(2025, 10, 23, 0, 0);
        SetApplyTermCommand command = new SetApplyTermCommand(
                2025, 11,
                sameTime,
                sameTime,
                1
        );

        // When & Then
        assertThatThrownBy(() -> monthlyScheduleConfigService.setApplyTerm(command))
                .isInstanceOf(ScheduleConfigException.class);

        verify(monthlyScheduleConfigRepository, never()).save(any(MonthlyScheduleConfig.class));
    }
}
