package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.ScheduleValidator;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
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

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    @DisplayName("모든 일정이 성공적으로 등록되는 경우")
    void applyWorkSchedules_AllSuccess() {
        // Given
        User mockUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(2)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("모든 일정 등록이 실패하는 경우 - ScheduleAllFailureException 발생")
    void applyWorkSchedules_AllFailure() {
        // Given
        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(ScheduleAllFailureException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("일부 일정 등록이 실패하는 경우 - SchedulePartialFailureException 발생")
    void applyWorkSchedules_PartialFailure() {
        // Given
        User mockUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(slot1)).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slot2)).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 이메일로 일정 등록 시도 - UserNotFoundException 발생")
    void applyWorkSchedules_UserNotFound() {
        // Given
        WorkScheduleCommand slot = new WorkScheduleCommand(
                "nonexistent@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(UserNotFoundException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
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
        verify(userRepository, never()).findByEmail(anyString());
        verify(scheduleValidator, never()).isScheduleInsertable(any());
    }

    @Test
    @DisplayName("동일한 사용자의 여러 시간대 일정 등록")
    void applyWorkSchedules_SameUserMultipleSlots() {
        // Given
        User mockUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        List<WorkScheduleCommand> slots = List.of(
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 9, 0),
                        LocalDateTime.of(2023, 10, 1, 10, 30)),
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 11, 0),
                        LocalDateTime.of(2023, 10, 1, 12, 30)),
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 14, 0),
                        LocalDateTime.of(2023, 10, 1, 16, 0))
        );

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(3);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(3)).save(any(WorkSchedule.class));
        verify(userRepository, times(3)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("서로 다른 사용자의 일정 등록")
    void applyWorkSchedules_DifferentUsers() {
        // Given
        User user1 = User.builder()
                .email("user1@example.com")
                .name("User 1")
                .build();

        User user2 = User.builder()
                .email("user2@example.com")
                .name("User 2")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "user1@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "user2@example.com",
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(1)).findByEmail("user1@example.com");
        verify(userRepository, times(1)).findByEmail("user2@example.com");
    }

    @Test
    @DisplayName("엣지케이스 - 일부 사용자만 존재하는 경우 첫 번째 실패 시 예외 발생")
    void applyWorkSchedules_MixedUserExistence() {
        // Given
        User user1 = User.builder()
                .email("user1@example.com")
                .name("User 1")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "user1@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "nonexistent@example.com",
                LocalDateTime.of(2023, 10, 1, 13, 0),
                LocalDateTime.of(2023, 10, 1, 17, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(UserNotFoundException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findByEmail("user1@example.com");
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("엣지케이스 - 동일 사용자가 같은 시간대에 여러 번 등록 시도")
    void applyWorkSchedules_SameUserSameTimeSlot() {
        // Given
        User mockUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        WorkScheduleCommand slot1 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );
        WorkScheduleCommand slot2 = new WorkScheduleCommand(
                "test@example.com",
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 12, 0)
        );

        List<WorkScheduleCommand> slots = List.of(slot1, slot2);

        // slot1과 slot2가 동일한 값이므로 순차적으로 반환하도록 설정
        when(scheduleValidator.isScheduleInsertable(any())).thenReturn(true, false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("엣지케이스 - 복잡한 실패 패턴 (성공-실패-성공-실패)")
    void applyWorkSchedules_ComplexFailurePattern() {
        // Given
        User mockUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        List<WorkScheduleCommand> slots = List.of(
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 9, 0),
                        LocalDateTime.of(2023, 10, 1, 10, 0)),
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 11, 0),
                        LocalDateTime.of(2023, 10, 1, 12, 0)),
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 13, 0),
                        LocalDateTime.of(2023, 10, 1, 14, 0)),
                new WorkScheduleCommand("test@example.com",
                        LocalDateTime.of(2023, 10, 1, 15, 0),
                        LocalDateTime.of(2023, 10, 1, 16, 0))
        );

        when(scheduleValidator.isScheduleInsertable(slots.get(0))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slots.get(1))).thenReturn(false);
        when(scheduleValidator.isScheduleInsertable(slots.get(2))).thenReturn(true);
        when(scheduleValidator.isScheduleInsertable(slots.get(3))).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
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
}