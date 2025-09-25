package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.application.schedule.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.application.schedule.dtos.WorkScheduleCommand;
import com.better.CommuteMate.application.schedule.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.application.schedule.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.domain.auth.repository.UserRepository;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.UserEntity;
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
        UserEntity mockUser = UserEntity.builder()
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
        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(2)).findById("test@example.com");
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
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("일부 일정 등록이 실패하는 경우 - SchedulePartialFailureException 발생")
    void applyWorkSchedules_PartialFailure() {
        // Given
        UserEntity mockUser = UserEntity.builder()
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
        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(SchedulePartialFailureException.class);

        verify(workSchedulesRepository, times(1)).save(any());
        verify(userRepository, times(1)).findById("test@example.com");
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
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> scheduleService.applyWorkSchedules(slots))
                .isInstanceOf(UserNotFoundException.class);

        verify(workSchedulesRepository, never()).save(any());
        verify(userRepository, times(1)).findById("nonexistent@example.com");
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
        verify(userRepository, never()).findById(anyString());
        verify(scheduleValidator, never()).isScheduleInsertable(any());
    }

    @Test
    @DisplayName("동일한 사용자의 여러 시간대 일정 등록")
    void applyWorkSchedules_SameUserMultipleSlots() {
        // Given
        UserEntity mockUser = UserEntity.builder()
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
        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(3);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(3)).save(any(WorkSchedule.class));
        verify(userRepository, times(3)).findById("test@example.com");
    }

    @Test
    @DisplayName("서로 다른 사용자의 일정 등록")
    void applyWorkSchedules_DifferentUsers() {
        // Given
        UserEntity user1 = UserEntity.builder()
                .email("user1@example.com")
                .name("User 1")
                .build();

        UserEntity user2 = UserEntity.builder()
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
        when(userRepository.findById("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2@example.com")).thenReturn(Optional.of(user2));
        when(workSchedulesRepository.save(any(WorkSchedule.class))).thenReturn(null);

        // When
        ApplyScheduleResultCommand result = scheduleService.applyWorkSchedules(slots);

        // Then
        assertThat(result.success()).hasSize(2);
        assertThat(result.fail()).isEmpty();
        verify(workSchedulesRepository, times(2)).save(any(WorkSchedule.class));
        verify(userRepository, times(1)).findById("user1@example.com");
        verify(userRepository, times(1)).findById("user2@example.com");
    }
}