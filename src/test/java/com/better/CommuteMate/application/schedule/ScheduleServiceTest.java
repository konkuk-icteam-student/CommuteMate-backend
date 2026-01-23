package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.ScheduleValidator;
import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleValidator scheduleValidator;

    @Mock
    private MonthlyScheduleConfigService monthlyScheduleConfigService;

    @Mock
    private WorkChangeRequestRepository workChangeRequestRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ScheduleService scheduleService;
    @BeforeEach
    void setUp() {
        lenient().when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("모든 일정이 성공적으로 등록되는 경우")
    void applyWorkSchedules_AllSuccess() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("모든 일정 등록이 실패하는 경우 - ScheduleAllFailureException 발생")
    void applyWorkSchedules_AllFailure() {
        // Given
        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(ScheduleAllFailureException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("일부 일정 등록이 실패하는 경우 - SchedulePartialFailureException 발생")
    void applyWorkSchedules_PartialFailure() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(slot1)).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slot2)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 일정 등록 시도 - UserNotFoundException 발생")
    void applyWorkSchedules_UserNotFound() {
        WorkScheduleCommand slot = new WorkScheduleCommand(
                999L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(ScheduleAllFailureException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("빈 슬롯 리스트로 일정 등록 시도 - ScheduleAllFailureException 발생")
    void applyWorkSchedules_EmptySlots() {
        // Given
        List<WorkScheduleCommand> emptySlots = List.of();

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(emptySlots))
                .isInstanceOf(ScheduleAllFailureException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
        verify(scheduleValidator, never()).isScheduleInsertable(any());
    }

    @Test
    @DisplayName("동일한 사용자의 여러 시간대 일정 등록")
    void applyWorkSchedules_SameUserMultipleSlots() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        List<WorkScheduleCommand> slots = List.of(
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 9, 0),
                        LocalDateTime.of(2023, 10, 1, 10, 30)),
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 11, 0),
                        LocalDateTime.of(2023, 10, 1, 12, 30)),
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 14, 0),
                        LocalDateTime.of(2023, 10, 1, 16, 0))
        );

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(3);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(3)).save(any(WorkSchedule.class));
        verify(userRepository, times(3)).findById(1L);
    }

    @Test
    @DisplayName("서로 다른 사용자의 일정 등록")
    void applyWorkSchedules_DifferentUsers() {
        // Given
        User user1 = User.builder()
                .userId(1L)
                .email("user1@example.com")
                .name("User 1")
                .build();

        User user2 = User.builder()
                .userId(2L)
                .email("user2@example.com")
                .name("User 2")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                2L,
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
    }

    @Test
    @DisplayName("엣지케이스 - 일부 사용자만 존재하는 경우 첫 번째 실패 시 예외 발생")
    void applyWorkSchedules_MixedUserExistence() {
        User user1 = User.builder()
                .userId(1L)
                .email("user1@example.com")
                .name("User 1")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                999L,
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("엣지케이스 - 동일 사용자가 같은 시간대에 여러 번 등록 시도")
    void applyWorkSchedules_SameUserSameTimeSlot() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                1L,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        // slot1과 slot2가 동일한 값이므로 순차적으로 반환하도록 설정
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("엣지케이스 - 복잡한 실패 패턴 (성공-실패-성공-실패)")
    void applyWorkSchedules_ComplexFailurePattern() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        List<WorkScheduleCommand> slots = List.of(
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 9, 0),
                        LocalDateTime.of(2023, 10, 1, 10, 0)),
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 11, 0),
                        LocalDateTime.of(2023, 10, 1, 12, 0)),
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 13, 0),
                        LocalDateTime.of(2023, 10, 1, 14, 0)),
                new WorkScheduleCommand(1L,
                        LocalDateTime.of(2023, 10, 1, 15, 0),
                        LocalDateTime.of(2023, 10, 1, 16, 0))
        );

        when(scheduleValidator.isScheduleInsertable(slots.get(0))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slots.get(1))).thenReturn(false);
        when(scheduleValidator.isScheduleInsertable(slots.get(2))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slots.get(3))).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class)
                .satisfies(exception -> {
                    SchedulePartialFailureException ex = (SchedulePartialFailureException) exception;
                    ScheduleResponseDetail detail = (ScheduleResponseDetail) ex.getErrorResponseDetail();
                    assertThat(detail.getSuccess()).hasSize(2);
                    assertThat(detail.getFailure()).hasSize(2);
                });

        verify(workSchedulesRepository, times(2)).save(any());
    }

    // ========================== modifyWorkSchedules 테스트 ==========================

    @Test
    @DisplayName("modifyWorkSchedules - 정상적인 일정 수정 (시간 일치, 검증 통과)")
    void modifyWorkSchedules_Success() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 12, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 13, 0),
                LocalDateTime.of(2026, 1, 20, 16, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);
        when(workChangeRequestRepository.save(any())).thenReturn(null);

        scheduleService.modifyWorkSchedules(modifyRequest, 1L);

        verify(workSchedulesRepository, times(1)).findById(100L);
        verify(workSchedulesRepository, times(1)).save(any(WorkSchedule.class));
        verify(workChangeRequestRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("modifyWorkSchedules - 시간 불일치로 실패 (취소 3시간, 추가 4시간)")
    void modifyWorkSchedules_TimeMismatch_Failure() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 12, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 9, 0),
                LocalDateTime.of(2026, 1, 20, 13, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);
        when(workChangeRequestRepository.save(any())).thenReturn(null);

        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(ScheduleAllFailureException.class);
    }

    @Test
    @DisplayName("modifyWorkSchedules - 최소 근무 시간 미달 (2시간 미만)")
    void modifyWorkSchedules_MinWorkTimeViolation_Failure() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 11, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 9, 0),
                LocalDateTime.of(2026, 1, 20, 10, 30)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        doThrow(BasicException.of(ScheduleErrorCode.MIN_WORK_TIME_NOT_MET)).when(scheduleValidator).validateMinWorkTime(any());

        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("modifyWorkSchedules - 월 총 근무 시간 초과 (27시간 초과)")
    void modifyWorkSchedules_MonthlyWorkTimeViolation_Failure() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 11, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 9, 0),
                LocalDateTime.of(2026, 1, 20, 11, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        doThrow(BasicException.of(ScheduleErrorCode.TOTAL_WORK_TIME_EXCEEDED)).when(scheduleValidator).validateTotalWorkTime(anyLong(), anyLong());

        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("modifyWorkSchedules - 주 최대 근무 시간 초과 (13시간 초과)")
    void modifyWorkSchedules_WeeklyWorkTimeViolation_Failure() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 11, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 16, 9, 0),
                LocalDateTime.of(2026, 1, 16, 11, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        doThrow(BasicException.of(ScheduleErrorCode.WEEKLY_WORK_TIME_EXCEEDED)).when(scheduleValidator).validateWeeklyWorkTime(anyLong(), anyLong());

        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("modifyWorkSchedules - 동시 근무자 수 초과로 실패")
    void modifyWorkSchedules_ConcurrentLimitExceeded_Failure() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule existingSchedule = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 11, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 9, 0),
                LocalDateTime.of(2026, 1, 20, 11, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(100L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(existingSchedule));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(ScheduleAllFailureException.class);
    }

    @Test
    @DisplayName("modifyWorkSchedules - 삭제할 일정이 존재하지 않는 경우")
    void modifyWorkSchedules_ScheduleNotFound_Failure() {
        // Given
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        // 추가할 슬롯
        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2025, 10, 20, 9, 0),
                LocalDateTime.of(2025, 10, 20, 11, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot),
                List.of(999L), // 존재하지 않는 ID
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> scheduleService.modifyWorkSchedules(modifyRequest, 1L))
                .isInstanceOf(ScheduleAllFailureException.class);

        verify(workSchedulesRepository, times(1)).findById(999L);
        verify(workSchedulesRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifyWorkSchedules - 여러 일정 취소 및 추가 (배치 처리)")
    void modifyWorkSchedules_MultipleCancelAndAdd_Success() {
        User mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkSchedule schedule1 = WorkSchedule.builder()
                .scheduleId(100L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 15, 11, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkSchedule schedule2 = WorkSchedule.builder()
                .scheduleId(101L)
                .user(mockUser)
                .startTime(LocalDateTime.of(2026, 1, 16, 13, 0))
                .endTime(LocalDateTime.of(2026, 1, 16, 15, 0))
                .statusCode(CodeType.WS02)
                .build();

        WorkScheduleDTO addSlot1 = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 20, 9, 0),
                LocalDateTime.of(2026, 1, 20, 11, 0)
        );

        WorkScheduleDTO addSlot2 = new WorkScheduleDTO(
                LocalDateTime.of(2026, 1, 21, 14, 0),
                LocalDateTime.of(2026, 1, 21, 16, 0)
        );

        ModifyWorkScheduleDTO modifyRequest = new ModifyWorkScheduleDTO(
                List.of(addSlot1, addSlot2),
                List.of(100L, 101L),
                "일정 변경 요청"
        );

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.findById(100L)).thenReturn(Optional.of(schedule1));
        when(workSchedulesRepository.findById(101L)).thenReturn(Optional.of(schedule2));
        when(monthlyScheduleConfigService.isCurrentlyInApplyTerm(any(LocalDateTime.class))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);
        when(workChangeRequestRepository.save(any())).thenReturn(null);

        scheduleService.modifyWorkSchedules(modifyRequest, 1L);

        verify(workSchedulesRepository, times(1)).findById(100L);
        verify(workSchedulesRepository, times(1)).findById(101L);
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(workChangeRequestRepository, times(4)).save(any());
    }
}
