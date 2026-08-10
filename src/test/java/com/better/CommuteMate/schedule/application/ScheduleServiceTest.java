package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private WorkSchedulesRepository workSchedulesRepository;
    @Mock private WorkAttendanceRepository workAttendanceRepository;
    @Mock private WorkChangeRequestItemRepository workChangeRequestItemRepository;
    @Mock private WorkChangeRequestRepository workChangeRequestRepository;
    @Mock private WorkUnavailableTimeRepository workUnavailableTimeRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkplaceRepository workplaceRepository;
    @Mock private ScheduleValidator scheduleValidator;
    @Mock private WorkScheduleSettingService workScheduleSettingService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ScheduleService scheduleService;

    // 09:00~11:00 (2시간 = 30분 단위 4개 슬롯)
    private final WorkScheduleSlotCommand slot = new WorkScheduleSlotCommand(
            LocalDate.of(2026, 8, 10),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0)
    );

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                workSchedulesRepository, workAttendanceRepository,
                workChangeRequestItemRepository, workChangeRequestRepository,
                workUnavailableTimeRepository, userRepository, workplaceRepository,
                scheduleValidator, workScheduleSettingService, messagingTemplate
        );
    }

    @Test
    @DisplayName("근무 신청 - 09:00~11:00 신청 시 30분 단위 4개 슬롯이 saveAll로 저장된다")
    void changeWorkSchedules_AddSlot_SplitsIntoUnitSlotsAndSavesAll() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workSchedulesRepository.existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeNot(
                1L, slot.date(), slot.start(), slot.end(), com.better.CommuteMate.global.code.CodeType.WS04))
                .thenReturn(false);
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(scheduleValidator.isScheduleInsertable(slot, setting)).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())
        );

        // 응답은 원본 범위(09:00~11:00) 기준 1건 성공
        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
        // 저장은 saveAll 배치로 처리
        verify(workSchedulesRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("근무 신청 - 단위 슬롯 중 하나라도 정원 초과면 원본 범위 전체가 실패한다")
    void changeWorkSchedules_ConcurrentLimitExceeded_WholeRangeFails() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(scheduleValidator.isScheduleInsertable(slot, setting)).thenReturn(false);

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())
        );

        assertThat(result.success()).isEmpty();
        assertThat(result.failure()).hasSize(1);
        verify(workSchedulesRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("근무 신청 - 추가와 삭제 슬롯이 모두 비어 있으면 예외가 발생한다")
    void changeWorkSchedules_EmptyRequest_ThrowsException() {
        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(), List.of())))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("근무 신청 - 시작 시각이 슬롯 경계가 아니면 400 예외가 발생한다")
    void changeWorkSchedules_NonBoundaryStart_ThrowsInvalidSlotUnit() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(eq(10L), eq(2026), eq(8)))
                .thenReturn(setting);

        // 09:15~09:45 : 시작이 슬롯 경계 아님 (30분 단위인데 15분 시작)
        WorkScheduleSlotCommand badSlot = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 15), LocalTime.of(9, 45));

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(badSlot), List.of())))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("근무 신청 - 길이가 최소 단위 배수가 아니면 400 예외가 발생한다")
    void changeWorkSchedules_NonMultipleDuration_ThrowsInvalidSlotUnit() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(eq(10L), eq(2026), eq(8)))
                .thenReturn(setting);

        // 09:00~09:45 : 45분은 30의 배수 아님
        WorkScheduleSlotCommand badSlot = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(9, 45));

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(badSlot), List.of())))
                .isInstanceOf(CustomException.class);
    }

    private WorkScheduleSetting setting() {
        return WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026)
                .month(8)
                .maxConcurrentWorkers(3)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .build();
    }
}
