package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.ScheduleValidator;
import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private com.better.CommuteMate.domain.user.repository.UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ScheduleValidator scheduleValidator;

    private User testUser;

    @InjectMocks
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .name("Test User")
                .build();

        lenient().when(userRepository.findByUserId(anyInt())).thenReturn(Optional.of(testUser));
        lenient().when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        lenient().when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
        lenient().when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        lenient().when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

        ReflectionTestUtils.setField(scheduleValidator, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 3);

        ReflectionTestUtils.setField(scheduleService, "scheduleValidator", scheduleValidator);
        ReflectionTestUtils.setField(scheduleService, "monthlyScheduleConfigService", monthlyScheduleConfigService);
    }

    // ========== 월별/주별 제한 조합 테스트 ==========

    @Test
    @DisplayName("복합 시나리오 - 주별 제한은 여유가 있지만 월별 제한 초과")
    @Disabled
    void complexScenario_MonthlyOk_WeeklyExceeded() {
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 13, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 10), 9, 0, 13, 0)
        );

        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 2), 14, 0, 22, 0)
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("복합 시나리오 - 월별/주별 제한 둘 다 초과")
    @Disabled
    void complexScenario_BothExceeded() {
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 21, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 23, 0)
        );

        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 21, 0)
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("복합 시나리오 - 주별 제한은 여유가 있지만 월별 제한 초과")
    @Disabled
    void complexScenario_WeeklyOk_MonthlyExceeded() {
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 13, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 10), 9, 0, 15, 0), // 6시간
                createWorkSchedule(LocalDate.of(2025, 11, 15), 9, 0, 21, 0)  // 12시간
        );

        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 15, 0)  // 6시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    // ========== 배치 처리 복잡한 케이스 ==========

    @Test
    @DisplayName("배치 처리 - 일부 성공, 일부 실패 (부분 성공)")
    @Disabled
    void batchProcessing_PartialSuccess() {
        WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)
        );

        WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 2, 9, 0),
                LocalDateTime.of(2025, 11, 2, 11, 0)
        );

        WorkScheduleCommand schedule3 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
        when(scheduleValidator.isScheduleInsertable(schedule1)).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(schedule2)).thenReturn(false);
        when(scheduleValidator.isScheduleInsertable(schedule3)).thenReturn(true);

        WorkSchedule saved1 = createWorkScheduleWithId(1, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
        WorkSchedule saved3 = createWorkScheduleWithId(3, LocalDate.of(2025, 11, 3), 9, 0, 11, 0);

        when(workSchedulesRepository.save(any())).thenReturn(saved1, saved3);

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(schedule1, schedule2, schedule3)))
                .isInstanceOf(com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException.class);
    }

    // ========== 수정 시 복잡한 케이스 ==========

    @Test
    @DisplayName("수정 복합 시나리오 - 취소 후 추가 시 주별 제한 재계산")
    @Disabled
    void modifyScenario_CancelAndAdd_WeeklyRecalculation() {
        WorkSchedule existingSchedule = createWorkScheduleWithId(1, LocalDate.of(2026, 1, 2), 9, 0, 15, 0);

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 3, 9, 0),
                LocalDateTime.of(2026, 1, 3, 17, 0)
        );

        ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(1),
                "일정 변경"
        );

        List<WorkSchedule> weeklySchedules = List.of(
                existingSchedule,
                createWorkScheduleWithId(2, LocalDate.of(2026, 1, 4), 9, 0, 15, 0)
        );

        when(workSchedulesRepository.findById(1)).thenReturn(Optional.of(existingSchedule));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existingSchedule));
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

        WorkSchedule canceledSchedule = createWorkScheduleWithId(1, LocalDate.of(2026, 1, 2), 9, 0, 15, 0);
        ReflectionTestUtils.setField(canceledSchedule, "statusCode", CodeType.WS04);
        when(workSchedulesRepository.save(existingSchedule)).thenReturn(canceledSchedule);

        WorkSchedule newSchedule = createWorkScheduleWithId(3, LocalDate.of(2026, 1, 3), 9, 0, 17, 0);
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(newSchedule);

        scheduleService.modifyWorkSchedules(request, 1);

        verify(workSchedulesRepository, times(1)).save(existingSchedule);
        verify(workChangeRequestRepository, times(1)).save(any());
    }

    // ========== 동시성 제한 복합 케이스 ==========

    @Test
    @DisplayName("동시성 제한 - 같은 시간대에 최대 인원 초과")
    @Disabled
    void concurrency_ExceedsMaxConcurrent() {
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)
        );

        List<WorkSchedule> overlappingSchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 8, 0, 10, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 1), 8, 30, 10, 30),
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
        );

        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);

        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        assertThat(result).isFalse();
    }

    // ========== 엣지 케이스 ==========

    @Test
    @DisplayName("엣지 케이스 - 월 경계 (10월 마지막 날 + 11월 첫 날)")
    @Disabled
    void edgeCase_MonthBoundary() {
        // Given
        WorkScheduleCommand octoberSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 10, 31, 20, 0),  // 10월 마지막 날
                LocalDateTime.of(2025, 10, 31, 22, 0)   // 2시간
        );

        WorkScheduleCommand novemberSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),    // 11월 첫 날
                LocalDateTime.of(2025, 11, 1, 11, 0)    // 2시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        // Weekly schedules will be returned by findValidSchedulesByUserAndDateRange mock
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
        when(workSchedulesRepository.save(any())).thenReturn(
                createWorkScheduleWithId(1, LocalDate.of(2025, 10, 31), 20, 0, 22, 0),
                createWorkScheduleWithId(2, LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
        );

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(
                List.of(octoberSchedule, novemberSchedule)
        );

        // Then
        assertThat(result.success()).hasSize(2);  // 둘 다 다른 월이므로 개별 카운트
        assertThat(result.fail()).isEmpty();
    }

    @Test
    @DisplayName("엣지 케이스 - 최소 시간 경계 (정확히 2시간)")
    @Disabled
    void edgeCase_MinimumWorkTime() {
        // Given
        WorkScheduleCommand exactlyTwoHours = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)  // 정확히 2시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        // Weekly schedules will be returned by findValidSchedulesByUserAndDateRange mock
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());

        WorkSchedule saved = createWorkScheduleWithId(1, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
        when(workSchedulesRepository.save(any())).thenReturn(saved);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(List.of(exactlyTwoHours));

        // Then
        assertThat(result.success()).hasSize(1);
        assertThat(result.fail()).isEmpty();
    }

    // ========== Helper Methods ==========

    private WorkSchedule createWorkSchedule(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        return WorkSchedule.builder()
                .user(testUser)
                .startTime(LocalDateTime.of(date, java.time.LocalTime.of(startHour, startMinute)))
                .endTime(LocalDateTime.of(date, java.time.LocalTime.of(endHour, endMinute)))
                .statusCode(CodeType.WS02)
                .build();
    }

    private WorkSchedule createWorkScheduleWithId(Integer id, LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        WorkSchedule schedule = createWorkSchedule(date, startHour, startMinute, endHour, endMinute);
        ReflectionTestUtils.setField(schedule, "scheduleId", id);
        return schedule;
    }
}
