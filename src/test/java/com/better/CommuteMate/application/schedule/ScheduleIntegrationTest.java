package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.ScheduleValidator;
import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 스케줄 모듈 통합 테스트
 * - 월별/주별 제한 조합 테스트
 * - 배치 처리 복잡한 케이스
 * - 수정 시 복잡한 시나리오
 * - 동시성 제한 복합 케이스
 * - 엣지 케이스 (경계값, 월/주 경계)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleIntegrationTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;

    @Mock
    private MonthlyScheduleConfigRepository monthlyScheduleConfigRepository;

    @Mock
    private MonthlyScheduleConfigService monthlyScheduleConfigService;

    @Mock
    private WorkChangeRequestRepository workChangeRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ScheduleValidator scheduleValidator;

    @InjectMocks
    private ScheduleService scheduleService;

    private User testUser;

    // 근무 시간 제한 상수 (분 단위)
    private static final long MAX_MONTHLY_MINUTES = 27 * 60; // 월 최대 27시간
    private static final long MAX_WEEKLY_MINUTES = 13 * 60;  // 주 최대 13시간
    private static final long MIN_SESSION_MINUTES = 2 * 60;  // 1회 최소 2시간

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .name("Test User")
                .build();

        // ScheduleValidator 기본 설정
        ReflectionTestUtils.setField(scheduleValidator, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 3);

        // ScheduleService에 의존성 주입
        ReflectionTestUtils.setField(scheduleService, "scheduleValidator", scheduleValidator);
        ReflectionTestUtils.setField(scheduleService, "monthlyScheduleConfigService", monthlyScheduleConfigService);
    }

    // ========== 월별 제한 테스트 ==========

    @Nested
    @DisplayName("월별 총 근무 시간 제한 테스트")
    class MonthlyWorkTimeLimitTest {

        @Test
        @DisplayName("월 총 근무 시간 27시간 초과 시 실패")
        void monthlyLimit_ExceedsMaxHours_ShouldFail() {
            // Given: 이미 26시간 근무 중, 2시간 추가 시도 = 28시간 (27시간 초과)
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 15, 9, 0),
                    LocalDateTime.of(2025, 11, 15, 11, 0) // 2시간
            );

            // 기존 월별 스케줄: 26시간
            List<WorkSchedule> existingMonthlySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 21, 0),  // 12시간
                    createWorkSchedule(LocalDate.of(2025, 11, 8), 9, 0, 23, 0)   // 14시간 = 총 26시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(existingMonthlySchedules);
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("월 총 근무 시간 27시간 이하면 성공")
        void monthlyLimit_WithinMaxHours_ShouldSucceed() {
            // Given: 이미 25시간 근무 중, 2시간 추가 시도 = 27시간 (허용)
            // 11/15가 포함된 주(11/10~11/16)에는 기존 스케줄 없음
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 15, 9, 0),
                    LocalDateTime.of(2025, 11, 15, 11, 0) // 2시간
            );

            // 기존 월별 스케줄: 25시간 (다른 주에 분산)
            List<WorkSchedule> existingMonthlySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 21, 0),  // 12시간 (11/1 주)
                    createWorkSchedule(LocalDate.of(2025, 11, 8), 9, 0, 22, 0)   // 13시간 (11/8 주) = 총 25시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            // 월별/주별 조회에 따라 다른 결과 반환
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenAnswer(invocation -> {
                        LocalDateTime start = invocation.getArgument(1);
                        LocalDateTime end = invocation.getArgument(2);
                        // 월 단위 조회 (1일~말일)인 경우 전체 반환
                        if (start.getDayOfMonth() == 1 &&
                            java.time.Duration.between(start, end).toDays() >= 28) {
                            return existingMonthlySchedules;
                        }
                        // 주 단위 조회 (11/10~11/16 주)인 경우 빈 리스트 반환
                        return List.of();
                    });
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule savedSchedule = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 15), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(savedSchedule);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(newSchedule));

            // Then
            assertThat(result.success()).hasSize(1);
            assertThat(result.fail()).isEmpty();
        }
    }

    // ========== 주별 제한 테스트 ==========

    @Nested
    @DisplayName("주별 총 근무 시간 제한 테스트")
    class WeeklyWorkTimeLimitTest {

        @Test
        @DisplayName("주 최대 근무 시간 13시간 초과 시 실패")
        void weeklyLimit_ExceedsMaxHours_ShouldFail() {
            // Given: 이미 12시간 근무 중, 2시간 추가 시도 = 14시간 (13시간 초과)
            // 2025년 11월 3일 = 월요일 (11월 1주차: 11/3 ~ 11/9)
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 5, 9, 0),  // 수요일
                    LocalDateTime.of(2025, 11, 5, 11, 0)  // 2시간
            );

            // 기존 주간 스케줄: 12시간 (같은 주)
            List<WorkSchedule> existingWeeklySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 3), 9, 0, 15, 0),  // 월요일 6시간
                    createWorkSchedule(LocalDate.of(2025, 11, 4), 9, 0, 15, 0)   // 화요일 6시간 = 12시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            // 월별 조회 시에도 같은 스케줄 반환 (같은 월)
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(existingWeeklySchedules);
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("주 최대 근무 시간 13시간 이하면 성공")
        void weeklyLimit_WithinMaxHours_ShouldSucceed() {
            // Given: 이미 11시간 근무 중, 2시간 추가 시도 = 13시간 (허용)
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 5, 9, 0),
                    LocalDateTime.of(2025, 11, 5, 11, 0) // 2시간
            );

            // 기존 주간 스케줄: 11시간
            List<WorkSchedule> existingWeeklySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 3), 9, 0, 15, 0),  // 6시간
                    createWorkSchedule(LocalDate.of(2025, 11, 4), 9, 0, 14, 0)   // 5시간 = 11시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(existingWeeklySchedules);
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule savedSchedule = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 5), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(savedSchedule);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(newSchedule));

            // Then
            assertThat(result.success()).hasSize(1);
            assertThat(result.fail()).isEmpty();
        }
    }

    // ========== 월별/주별 복합 시나리오 테스트 ==========

    @Nested
    @DisplayName("월별/주별 제한 복합 시나리오 테스트")
    class CombinedLimitTest {

        @Test
        @DisplayName("주별 제한은 여유가 있지만 월별 제한 초과 시 실패")
        void weeklyOk_MonthlyExceeded_ShouldFail() {
            // Given: 월 26시간 (1, 8, 15일에 분산), 주 6시간
            // 2시간 추가 시: 월 28시간 (초과), 주 8시간 (OK)
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 17, 9, 0),  // 월요일 (11/17 ~ 11/23 주)
                    LocalDateTime.of(2025, 11, 17, 11, 0)  // 2시간
            );

            // 이번 주에는 6시간만 있음 (주 제한 OK), 하지만 월 총 26시간
            List<WorkSchedule> monthlySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 17, 0),   // 8시간
                    createWorkSchedule(LocalDate.of(2025, 11, 8), 9, 0, 21, 0),   // 12시간
                    createWorkSchedule(LocalDate.of(2025, 11, 15), 9, 0, 15, 0)   // 6시간 = 총 26시간
            );

            // 이번 주 (11/17 포함 주)에는 아직 없음
            List<WorkSchedule> weeklySchedules = List.of();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenAnswer(invocation -> {
                        LocalDateTime start = invocation.getArgument(1);
                        // 월 시작일로 조회하면 월별 스케줄 반환
                        if (start.getDayOfMonth() == 1) {
                            return monthlySchedules;
                        }
                        // 그 외는 주별 (빈 리스트)
                        return weeklySchedules;
                    });
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("월별 제한은 여유가 있지만 주별 제한 초과 시 실패")
        void monthlyOk_WeeklyExceeded_ShouldFail() {
            // Given: 월 10시간, 주 12시간
            // 2시간 추가 시: 월 12시간 (OK), 주 14시간 (초과)
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 5, 9, 0),  // 수요일
                    LocalDateTime.of(2025, 11, 5, 11, 0)  // 2시간
            );

            // 같은 주(11/3~11/9)에 12시간 있음
            List<WorkSchedule> weeklySchedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 3), 9, 0, 15, 0),  // 월요일 6시간
                    createWorkSchedule(LocalDate.of(2025, 11, 4), 9, 0, 15, 0)   // 화요일 6시간 = 12시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            // 월별/주별 모두 같은 스케줄 반환 (해당 주가 11월 초)
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(weeklySchedules);
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("월별/주별 제한 둘 다 초과 시 실패 (월별 먼저 검증)")
        void bothExceeded_ShouldFail() {
            // Given: 월 26시간, 주 12시간 - 둘 다 초과
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 5, 9, 0),
                    LocalDateTime.of(2025, 11, 5, 11, 0) // 2시간
            );

            // 월 26시간 + 주 12시간 (같은 주에 집중)
            List<WorkSchedule> schedules = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 3), 9, 0, 21, 0),  // 12시간
                    createWorkSchedule(LocalDate.of(2025, 11, 10), 9, 0, 23, 0)  // 14시간 = 월 26시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(schedules);
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then: 월별 제한이 먼저 검증되므로 예외 발생
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }
    }

    // ========== 배치 처리 테스트 ==========

    @Nested
    @DisplayName("배치 처리 테스트")
    class BatchProcessingTest {

        @Test
        @DisplayName("배치 처리 - 일부 성공, 일부 실패 (부분 성공)")
        void batchProcessing_PartialSuccess() {
            // Given: 3개 신청 중 2번째만 동시 인원 초과로 실패
            WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );
            WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 2, 9, 0),
                    LocalDateTime.of(2025, 11, 2, 11, 0)
            );
            WorkScheduleCommand schedule3 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 3, 9, 0),
                    LocalDateTime.of(2025, 11, 3, 11, 0)
            );

            // 11/2에만 동시 인원 3명 꽉 참
            List<WorkSchedule> overlappingSchedules = List.of(
                    createWorkScheduleWithDifferentUser(2L, LocalDate.of(2025, 11, 2), 8, 0, 12, 0),
                    createWorkScheduleWithDifferentUser(3L, LocalDate.of(2025, 11, 2), 8, 30, 11, 30),
                    createWorkScheduleWithDifferentUser(4L, LocalDate.of(2025, 11, 2), 9, 0, 11, 0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // findByDate: 11/2에만 꽉 참
            when(workSchedulesRepository.findByDate(any(), any())).thenAnswer(invocation -> {
                LocalDateTime start = invocation.getArgument(0);
                if (start.getDayOfMonth() == 2) {
                    return overlappingSchedules;
                }
                return List.of();
            });

            WorkSchedule saved1 = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
            WorkSchedule saved3 = createWorkScheduleWithId(3L, LocalDate.of(2025, 11, 3), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(saved1, saved3);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(schedule1, schedule2, schedule3)))
                    .isInstanceOf(SchedulePartialFailureException.class)
                    .satisfies(ex -> {
                        SchedulePartialFailureException e = (SchedulePartialFailureException) ex;
                        // 부분 성공 확인은 예외 메시지 또는 details에서 확인 가능
                    });
        }

        @Test
        @DisplayName("배치 처리 - 전체 실패")
        void batchProcessing_AllFail() {
            // Given: 모든 일정이 동시 인원 초과
            WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );
            WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 10, 0),
                    LocalDateTime.of(2025, 11, 1, 12, 0)
            );

            // 모든 시간대에 동시 인원 초과
            List<WorkSchedule> overlappingSchedules = List.of(
                    createWorkScheduleWithDifferentUser(2L, LocalDate.of(2025, 11, 1), 8, 0, 14, 0),
                    createWorkScheduleWithDifferentUser(3L, LocalDate.of(2025, 11, 1), 8, 0, 14, 0),
                    createWorkScheduleWithDifferentUser(4L, LocalDate.of(2025, 11, 1), 8, 0, 14, 0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(schedule1, schedule2)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("배치 처리 - 전체 성공")
        void batchProcessing_AllSuccess() {
            // Given
            WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );
            WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 2, 9, 0),
                    LocalDateTime.of(2025, 11, 2, 11, 0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule saved1 = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
            WorkSchedule saved2 = createWorkScheduleWithId(2L, LocalDate.of(2025, 11, 2), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(saved1, saved2);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(schedule1, schedule2));

            // Then
            assertThat(result.success()).hasSize(2);
            assertThat(result.fail()).isEmpty();
        }

        @Test
        @DisplayName("배치 내 누적 시간 계산 - 같은 주에 여러 일정 신청")
        void batchProcessing_CumulativeWeeklyTime() {
            // Given: 주 11시간 있는 상태, 배치로 2시간 + 2시간 신청 = 15시간 (13시간 초과)
            WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 5, 9, 0),  // 수요일
                    LocalDateTime.of(2025, 11, 5, 11, 0)  // 2시간
            );
            WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 6, 9, 0),  // 목요일
                    LocalDateTime.of(2025, 11, 6, 11, 0)  // 2시간
            );

            // 기존 11시간
            List<WorkSchedule> existingWeekly = List.of(
                    createWorkSchedule(LocalDate.of(2025, 11, 3), 9, 0, 15, 0),  // 6시간
                    createWorkSchedule(LocalDate.of(2025, 11, 4), 9, 0, 14, 0)   // 5시간 = 11시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(existingWeekly);
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule saved1 = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 5), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(saved1);

            // When & Then: 첫 번째는 성공 (11+2=13), 두 번째는 실패 (13+2=15>13)
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(schedule1, schedule2)))
                    .isInstanceOf(SchedulePartialFailureException.class);
        }
    }

    // ========== 동시성 제한 테스트 ==========

    @Nested
    @DisplayName("동시 근무자 수 제한 테스트")
    class ConcurrencyLimitTest {

        @Test
        @DisplayName("동시 근무자 수 초과 시 실패")
        void concurrency_ExceedsMaxConcurrent_ShouldFail() {
            // Given: 최대 3명 동시 근무, 이미 3명 있는 시간대에 신청
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );

            // 9:00~11:00 시간대에 이미 3명
            List<WorkSchedule> overlappingSchedules = List.of(
                    createWorkScheduleWithDifferentUser(2L, LocalDate.of(2025, 11, 1), 8, 0, 12, 0),
                    createWorkScheduleWithDifferentUser(3L, LocalDate.of(2025, 11, 1), 8, 30, 11, 30),
                    createWorkScheduleWithDifferentUser(4L, LocalDate.of(2025, 11, 1), 9, 0, 10, 30)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("동시 근무자 수 이하면 성공")
        void concurrency_WithinLimit_ShouldSucceed() {
            // Given: 최대 3명 동시 근무, 2명 있는 시간대에 신청
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );

            List<WorkSchedule> overlappingSchedules = List.of(
                    createWorkScheduleWithDifferentUser(2L, LocalDate.of(2025, 11, 1), 8, 0, 12, 0),
                    createWorkScheduleWithDifferentUser(3L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule savedSchedule = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(savedSchedule);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(newSchedule));

            // Then
            assertThat(result.success()).hasSize(1);
        }

        @Test
        @DisplayName("월별 설정된 최대 동시 인원 적용")
        void concurrency_CustomMaxConcurrent() {
            // Given: 11월 최대 동시 인원 5명으로 설정
            WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );

            // 4명 있는 상태 (기본 3명 초과지만, 설정된 5명 미만)
            List<WorkSchedule> overlappingSchedules = List.of(
                    createWorkScheduleWithDifferentUser(2L, LocalDate.of(2025, 11, 1), 8, 0, 12, 0),
                    createWorkScheduleWithDifferentUser(3L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0),
                    createWorkScheduleWithDifferentUser(4L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0),
                    createWorkScheduleWithDifferentUser(5L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
            );

            MonthlyScheduleConfig config = MonthlyScheduleConfig.builder()
                    .scheduleYear(2025)
                    .scheduleMonth(11)
                    .maxConcurrent(5)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(2025, 11))
                    .thenReturn(Optional.of(config));
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule savedSchedule = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(savedSchedule);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(newSchedule));

            // Then: 5명 설정이므로 4명+1명 = 5명 OK
            assertThat(result.success()).hasSize(1);
        }
    }

    // ========== 엣지 케이스 테스트 ==========

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("최소 근무 시간 2시간 미만 시 실패")
        void minWorkTime_LessThanTwoHours_ShouldFail() {
            // Given: 1시간 30분 근무 신청 (2시간 미만)
            WorkScheduleCommand shortSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 10, 30)  // 1시간 30분
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(shortSchedule)))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }

        @Test
        @DisplayName("정확히 2시간 근무 시 성공")
        void minWorkTime_ExactlyTwoHours_ShouldSucceed() {
            // Given: 정확히 2시간 근무 신청
            WorkScheduleCommand exactlyTwoHours = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)  // 정확히 2시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule saved = createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(saved);

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(exactlyTwoHours));

            // Then
            assertThat(result.success()).hasSize(1);
            assertThat(result.fail()).isEmpty();
        }

        @Test
        @DisplayName("월 경계 - 다른 달의 일정은 별도 계산")
        void monthBoundary_DifferentMonthsCountedSeparately() {
            // Given: 10월 마지막 날 + 11월 첫 날 신청
            WorkScheduleCommand octoberSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 10, 31, 9, 0),
                    LocalDateTime.of(2025, 10, 31, 11, 0)
            );
            WorkScheduleCommand novemberSchedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 1, 9, 0),
                    LocalDateTime.of(2025, 11, 1, 11, 0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            when(workSchedulesRepository.save(any())).thenReturn(
                    createWorkScheduleWithId(1L, LocalDate.of(2025, 10, 31), 9, 0, 11, 0),
                    createWorkScheduleWithId(2L, LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
            );

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(
                    List.of(octoberSchedule, novemberSchedule)
            );

            // Then: 각 월별로 따로 계산되므로 둘 다 성공
            assertThat(result.success()).hasSize(2);
            assertThat(result.fail()).isEmpty();
        }

        @Test
        @DisplayName("주 경계 - 다른 주의 일정은 별도 계산")
        void weekBoundary_DifferentWeeksCountedSeparately() {
            // Given: 1주차 마지막 날(일요일) + 2주차 첫 날(월요일)
            // 2025년 11월 9일 = 일요일 (1주차 끝), 11월 10일 = 월요일 (2주차 시작)
            WorkScheduleCommand week1Schedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 9, 9, 0),   // 일요일
                    LocalDateTime.of(2025, 11, 9, 21, 0)   // 12시간
            );
            WorkScheduleCommand week2Schedule = new WorkScheduleCommand(
                    1L,
                    LocalDateTime.of(2025, 11, 10, 9, 0),  // 월요일
                    LocalDateTime.of(2025, 11, 10, 21, 0)  // 12시간
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            when(workSchedulesRepository.save(any())).thenReturn(
                    createWorkScheduleWithId(1L, LocalDate.of(2025, 11, 9), 9, 0, 21, 0),
                    createWorkScheduleWithId(2L, LocalDate.of(2025, 11, 10), 9, 0, 21, 0)
            );

            // When
            ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(
                    List.of(week1Schedule, week2Schedule)
            );

            // Then: 각 주별로 12시간씩이므로 13시간 제한 미만 → 둘 다 성공
            assertThat(result.success()).hasSize(2);
            assertThat(result.fail()).isEmpty();
        }
    }

    // ========== 수정 시나리오 테스트 ==========

    @Nested
    @DisplayName("수정 시나리오 테스트")
    class ModifyScenarioTest {

        @Test
        @DisplayName("수정 시 취소/추가 시간이 일치해야 성공")
        void modify_MatchingDuration_ShouldSucceed() {
            // Given: 4시간 취소, 4시간 추가
            WorkSchedule existingSchedule = createWorkScheduleWithId(1L, LocalDate.of(2026, 1, 2), 9, 0, 13, 0);
            ReflectionTestUtils.setField(existingSchedule, "user", testUser);

            WorkScheduleDTO addSlot = new WorkScheduleDTO(
                    LocalDateTime.of(2026, 1, 3, 9, 0),
                    LocalDateTime.of(2026, 1, 3, 13, 0)  // 4시간
            );

            ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                    List.of(addSlot),
                    List.of(1L),
                    "일정 변경"
            );

            when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findById(1L)).thenReturn(Optional.of(existingSchedule));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of(existingSchedule));
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule newSchedule = createWorkScheduleWithId(2L, LocalDate.of(2026, 1, 3), 9, 0, 13, 0);
            when(workSchedulesRepository.save(any())).thenReturn(newSchedule);
            when(workChangeRequestRepository.save(any())).thenReturn(mock(WorkChangeRequest.class));

            // When & Then: 예외 없이 정상 수행
            scheduleService.modifyWorkSchedules(request, 1L);

            // 삭제는 엔티티 상태 변경만 (save 호출 X), 추가만 save 호출
            verify(workSchedulesRepository, times(1)).save(any());
            // 삭제 요청 1회 + 추가 요청 1회 = 총 2회
            verify(workChangeRequestRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("수정 시 취소/추가 시간 불일치 시 실패")
        void modify_MismatchedDuration_ShouldFail() {
            // Given: 4시간 취소, 2시간 추가 (불일치)
            WorkSchedule existingSchedule = createWorkScheduleWithId(1L, LocalDate.of(2026, 1, 2), 9, 0, 13, 0);
            ReflectionTestUtils.setField(existingSchedule, "user", testUser);

            WorkScheduleDTO addSlot = new WorkScheduleDTO(
                    LocalDateTime.of(2026, 1, 3, 9, 0),
                    LocalDateTime.of(2026, 1, 3, 11, 0)  // 2시간 (4시간과 불일치)
            );

            ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                    List.of(addSlot),
                    List.of(1L),
                    "일정 변경"
            );

            when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(workSchedulesRepository.findById(1L)).thenReturn(Optional.of(existingSchedule));
            when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of(existingSchedule));
            when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
            when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

            WorkSchedule newSchedule = createWorkScheduleWithId(2L, LocalDate.of(2026, 1, 3), 9, 0, 11, 0);
            when(workSchedulesRepository.save(any())).thenReturn(newSchedule);
            when(workChangeRequestRepository.save(any())).thenReturn(mock(WorkChangeRequest.class));

            // When & Then
            assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(request, 1L))
                    .isInstanceOf(ScheduleAllFailureException.class);
        }
    }

    // ========== Helper Methods ==========

    private WorkSchedule createWorkSchedule(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        WorkSchedule schedule = WorkSchedule.builder()
                .user(testUser)
                .startTime(LocalDateTime.of(date, LocalTime.of(startHour, startMinute)))
                .endTime(LocalDateTime.of(date, LocalTime.of(endHour, endMinute)))
                .statusCode(CodeType.WS02)
                .isDeleted(false)
                .build();
        return schedule;
    }

    private WorkSchedule createWorkScheduleWithId(Long id, LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        WorkSchedule schedule = createWorkSchedule(date, startHour, startMinute, endHour, endMinute);
        ReflectionTestUtils.setField(schedule, "scheduleId", id);
        return schedule;
    }

    private WorkSchedule createWorkScheduleWithDifferentUser(Long userId, LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        User otherUser = User.builder().userId(userId).name("User " + userId).build();
        WorkSchedule schedule = WorkSchedule.builder()
                .user(otherUser)
                .startTime(LocalDateTime.of(date, LocalTime.of(startHour, startMinute)))
                .endTime(LocalDateTime.of(date, LocalTime.of(endHour, endMinute)))
                .statusCode(CodeType.WS02)
                .isDeleted(false)
                .build();
        return schedule;
    }
}
