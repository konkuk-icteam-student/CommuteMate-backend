package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;
    @Mock
    private WorkAttendanceRepository workAttendanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkplaceRepository workplaceRepository;
    @Mock
    private ScheduleValidator scheduleValidator;
    @Mock
    private WorkScheduleSettingService workScheduleSettingService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ScheduleService scheduleService;
    private WorkScheduleSlotCommand slot;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                workSchedulesRepository,
                workAttendanceRepository,
                userRepository,
                workplaceRepository,
                scheduleValidator,
                workScheduleSettingService,
                messagingTemplate
        );
        slot = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        );
    }

    @Test
    @DisplayName("근무 신청 - 유효한 슬롯을 신청하면 성공 목록에 포함된다")
    void changeWorkSchedules_AddSlot_Success() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workSchedulesRepository.existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeNot(
                1L, slot.date(), slot.start(), slot.end(), com.better.CommuteMate.global.code.CodeType.WS04))
                .thenReturn(false);
        when(workScheduleSettingService.getRequiredSetting("10", 2026, 8)).thenReturn(setting);
        when(scheduleValidator.isScheduleInsertable(slot, setting)).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId("10"))
                .thenReturn(Optional.of(Workplace.builder().organizationId("10").name("본사").build()));

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())
        );

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
        verify(workSchedulesRepository).save(any(WorkSchedule.class));
    }

    @Test
    @DisplayName("근무 신청 - 동시 근무 제한을 초과한 슬롯은 실패 목록에 포함된다")
    void changeWorkSchedules_ConcurrentLimitExceeded_Failure() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting("10", 2026, 8)).thenReturn(setting);
        when(scheduleValidator.isScheduleInsertable(slot, setting)).thenReturn(false);

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())
        );

        assertThat(result.success()).isEmpty();
        assertThat(result.failure()).hasSize(1);
        verify(workSchedulesRepository, never()).save(any());
    }

    @Test
    @DisplayName("근무 신청 - 추가와 삭제 슬롯이 모두 비어 있으면 실패한다")
    void changeWorkSchedules_EmptyRequest_ThrowsException() {
        WorkScheduleChangeCommand command = new WorkScheduleChangeCommand(1L, List.of(), List.of());

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(command))
                .isInstanceOf(com.better.CommuteMate.global.exceptions.CustomException.class);
    }

    private WorkScheduleSetting setting() {
        return WorkScheduleSetting.builder()
                .organizationId("10")
                .year(2026)
                .month(8)
                .maxConcurrentWorkers(3)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .build();
    }
}
