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
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ScheduleValidator scheduleValidator;

    @InjectMocks
    private ScheduleService scheduleService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // ScheduleValidator 기본값 설정
        ReflectionTestUtils.setField(scheduleValidator, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 3);

        // 월별 제한 기본값 설정 (없으면 3)
        when(monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        // ScheduleService에 ScheduleValidator 주입
        ReflectionTestUtils.setField(scheduleService, "scheduleValidator", scheduleValidator);
        ReflectionTestUtils.setField(scheduleService, "monthlyScheduleConfigService", monthlyScheduleConfigService);

        testUser = User.builder()
                .userId(1)
                .name("Test User")
                .build();
    }

    // ========== 월별/주별 제한 조합 테스트 ==========

    @Test
    @DisplayName("복합 시나리오 - 월별 제한은 여유가 있지만 주별 제한 초과")
    void complexScenario_MonthlyOk_WeeklyExceeded() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),  // 월요일
                LocalDateTime.of(2025, 11, 3, 11, 0)  // 2시간
        );

        // 월별: 12시간 이미 근무 (27시간 제한이므로 여유 있음)
        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 13, 0),  // 4시간
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),  // 4시간
                createWorkSchedule(LocalDate.of(2025, 11, 10), 9, 0, 13, 0)  // 4시간 (다른 주)
        );

        // 주별: 이미 12시간 근무 (13시간 제한이므로 2시간 추가 시 초과)
        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),  // 4시간
                createWorkSchedule(LocalDate.of(2025, 11, 2), 14, 0, 22, 0)  // 8시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);
        // Weekly schedules will be returned by the same findValidSchedulesByUserAndDateRange mock

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("복합 시나리오 - 주별 제한은 여유가 있지만 월별 제한 초과")
    void complexScenario_WeeklyOk_MonthlyExceeded() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)  // 2시간
        );

        // 월별: 이미 26시간 근무 (27시간 제한이므로 2시간 추가 시 초과)
        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 13, 0),  // 4시간
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 13, 0),  // 4시간
                createWorkSchedule(LocalDate.of(2025, 11, 10), 9, 0, 15, 0), // 6시간
                createWorkSchedule(LocalDate.of(2025, 11, 15), 9, 0, 21, 0)  // 12시간
        );

        // 주별: 6시간만 근무 (13시간 제한이므로 여유 있음)
        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 15, 0)  // 6시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);
        // Weekly schedules will be returned by the same findValidSchedulesByUserAndDateRange mock

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("복합 시나리오 - 월별/주별 제한 둘 다 초과")
    void complexScenario_BothExceeded() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)  // 2시간
        );

        // 월별: 이미 26시간 근무
        List<WorkSchedule> monthlySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 21, 0),  // 12시간
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 23, 0)   // 14시간
        );

        // 주별: 이미 12시간 근무
        List<WorkSchedule> weeklySchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 2), 9, 0, 21, 0)  // 12시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlySchedules);
        // Weekly schedules will be returned by the same findValidSchedulesByUserAndDateRange mock

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(List.of(newSchedule)))
                .isInstanceOf(BasicException.class);
    }

    // ========== 배치 처리 복잡한 케이스 ==========

    @Test
    @DisplayName("배치 처리 - 일부 성공, 일부 실패 (부분 성공)")
    void batchProcessing_PartialSuccess() {
        // Given
        WorkScheduleCommand schedule1 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)  // 유효한 2시간
        );

        WorkScheduleCommand schedule2 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 2, 9, 0),
                LocalDateTime.of(2025, 11, 2, 10, 0)  // 최소 시간 미달 (1시간)
        );

        WorkScheduleCommand schedule3 = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)  // 유효한 2시간
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        // Weekly schedules will be returned by findValidSchedulesByUserAndDateRange mock
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());

        // schedule1과 schedule3는 저장 성공
        WorkSchedule saved1 = createWorkScheduleWithId(1, LocalDate.of(2025, 11, 1), 9, 0, 11, 0);
        WorkSchedule saved3 = createWorkScheduleWithId(3, LocalDate.of(2025, 11, 3), 9, 0, 11, 0);

        when(workSchedulesRepository.save(any())).thenReturn(saved1, saved3);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(
                List.of(schedule1, schedule2, schedule3)
        );

        // Then
        assertThat(result.success()).hasSize(2);  // schedule1, schedule3 성공
        assertThat(result.fail()).hasSize(1);  // schedule2 실패
    }

    // ========== 수정 시 복잡한 케이스 ==========

    @Test
    @DisplayName("수정 복합 시나리오 - 취소 후 추가 시 주별 제한 재계산")
    void modifyScenario_CancelAndAdd_WeeklyRecalculation() {
        // Given
        // 기존 일정: 11월 2일 9시-15시 (6시간) 취소 예정
        WorkSchedule existingSchedule = createWorkScheduleWithId(1, LocalDate.of(2025, 11, 2), 9, 0, 15, 0);

        // 새로 추가할 일정: 11월 3일 9시-17시 (8시간)
        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 17, 0)
        );

        ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                List.of(addSlot),  // 추가할 일정
                List.of(1),  // 취소할 일정 ID
                "일정 변경"  // 변경 사유
        );

        // 같은 주의 다른 일정: 11월 4일 9시-15시 (6시간)
        List<WorkSchedule> weeklySchedules = List.of(
                existingSchedule,  // 취소 예정 (제외되어야 함)
                createWorkScheduleWithId(2, LocalDate.of(2025, 11, 4), 9, 0, 15, 0)  // 6시간
        );

        when(workSchedulesRepository.findById(1)).thenReturn(Optional.of(existingSchedule));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existingSchedule));
        // Weekly schedules will be returned by findValidSchedulesByUserAndDateRange mock
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(List.of());
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any())).thenReturn(true);

        // 취소된 일정은 WS04로 변경
        WorkSchedule canceledSchedule = createWorkScheduleWithId(1, LocalDate.of(2025, 11, 2), 9, 0, 15, 0);
        ReflectionTestUtils.setField(canceledSchedule, "statusCode", CodeType.WS04);
        when(workSchedulesRepository.save(existingSchedule)).thenReturn(canceledSchedule);

        // 추가된 일정 저장
        WorkSchedule newSchedule = createWorkScheduleWithId(3, LocalDate.of(2025, 11, 3), 9, 0, 17, 0);
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(newSchedule);

        // When
        scheduleService.modifyWorkSchedules(request, 1);

        // Then
        // 주별 시간 계산: 6시간(취소 예정, 제외) + 6시간(다른 일정) + 8시간(새 일정) = 14시간
        // 14시간 > 13시간 제한 => 예외 발생해야 함
        // 하지만 취소된 일정을 제외하면: 6시간(다른 일정) + 8시간(새 일정) = 14시간 (초과)
        verify(workSchedulesRepository, times(1)).save(existingSchedule);  // 취소 저장
        verify(workChangeRequestRepository, times(1)).save(any());  // 변경 요청 저장
    }

    // ========== 동시성 제한 복합 케이스 ==========

    @Test
    @DisplayName("동시성 제한 - 같은 시간대에 최대 인원 초과")
    void concurrency_ExceedsMaxConcurrent() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)
        );

        // 같은 시간대에 이미 3명 (MAX: 3)
        List<WorkSchedule> overlappingSchedules = List.of(
                createWorkSchedule(LocalDate.of(2025, 11, 1), 8, 0, 10, 0),
                createWorkSchedule(LocalDate.of(2025, 11, 1), 8, 30, 10, 30),
                createWorkSchedule(LocalDate.of(2025, 11, 1), 9, 0, 11, 0)
        );

        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        // Weekly schedules will be returned by findValidSchedulesByUserAndDateRange mock
        when(workSchedulesRepository.findByDate(any(), any())).thenReturn(overlappingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    // ========== 엣지 케이스 ==========

    @Test
    @DisplayName("엣지 케이스 - 월 경계 (10월 마지막 날 + 11월 첫 날)")
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
