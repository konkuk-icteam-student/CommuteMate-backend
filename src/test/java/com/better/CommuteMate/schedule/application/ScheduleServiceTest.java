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
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditRequest;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditResponse;
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
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
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
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(false);

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
        when(workScheduleSettingService.getRequiredSetting(eq(10L), eq(2026), eq(8)))
                .thenReturn(setting);

        // 09:00~09:45 : 45분은 30의 배수 아님
        WorkScheduleSlotCommand badSlot = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(9, 45));

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(badSlot), List.of())))
                .isInstanceOf(CustomException.class);
    }

    // ── apply 검증 회귀 및 신규 케이스 ────────────────────────────────

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 90분(09:00~10:30) 신청이 통과된다")
    void changeWorkSchedules_90min_WithMin60_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleSlotCommand slot90 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 30));
        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot90), List.of()));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
    }

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 150분(09:00~11:30) 신청이 통과된다")
    void changeWorkSchedules_150min_WithMin60_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleSlotCommand slot150 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(11, 30));
        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot150), List.of()));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
    }

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 30분(09:00~09:30) 단독 신청은 하한 미달로 반려된다")
    void changeWorkSchedules_30min_WithMin60_ThrowsInvalidSlotDuration() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());

        WorkScheduleSlotCommand slot30 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(9, 30));
        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot30), List.of())))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 끊긴 두 구간(각 30분)은 각각 하한 미달로 반려된다")
    void changeWorkSchedules_TwoDisjoint30min_WithMin60_ThrowsInvalidSlotDuration() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());

        WorkScheduleSlotCommand slot1 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(9, 30));
        WorkScheduleSlotCommand slot2 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot1, slot2), List.of())))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=30 설정에서 90분(09:00~10:30) 신청이 통과된다 (회귀)")
    void changeWorkSchedules_90min_WithMin30_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleSlotCommand slot90 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 30));
        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot90), List.of()));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
    }

    // ── edit 검증 케이스 ──────────────────────────────────────────────

    @Test
    @DisplayName("수정 신청 - minWorkUnitMinutes=60 설정에서 90분(09:00~10:30) 추가 신청이 통과된다")
    void submitEditRequest_90minAdd_WithMin60_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.of(setting60()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workChangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // deleteSlots 없음: 추가 슬롯 검증만 집중 테스트
        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 30))),
                "테스트 사유"
        );

        WorkScheduleEditResponse result = scheduleService.submitEditRequest(1L, request);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("수정 신청 - minWorkUnitMinutes=60 설정에서 30분(09:00~09:30) 추가는 하한 미달로 반려된다")
    void submitEditRequest_30minAdd_WithMin60_ThrowsInvalidSlotDuration() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.of(setting60()));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(9, 30))),
                "테스트 사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("수정 신청 - 09:15 같은 30분 비경계 추가는 정렬 위반으로 반려된다")
    void submitEditRequest_NonBoundaryStart_ThrowsInvalidSlotBoundary() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.of(setting()));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 15), LocalTime.of(9, 45))),
                "테스트 사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class);
    }

    // ── 저장/삭제 분할 단위 검증 ─────────────────────────────────────

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 90분(09:00~10:30) 신청 시 30분 슬롯 3개로 저장된다")
    void changeWorkSchedules_90min_WithMin60_SavesThreeSlots() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleSlotCommand slot90 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 30));
        scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot90), List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(workSchedulesRepository).saveAll(captor.capture());
        List<WorkSchedule> saved = captor.getValue();

        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.get(0).getEndTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(saved.get(1).getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(saved.get(1).getEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.get(2).getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.get(2).getEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("근무 신청 - minWorkUnitMinutes=60 설정에서 150분(09:00~11:30) 신청 시 30분 슬롯 5개로 저장된다")
    void changeWorkSchedules_150min_WithMin60_SavesFiveSlots() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting60());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleSlotCommand slot150 = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(11, 30));
        scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot150), List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(workSchedulesRepository).saveAll(captor.capture());
        List<WorkSchedule> saved = captor.getValue();

        assertThat(saved).hasSize(5);
        assertThat(saved.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.get(0).getEndTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(saved.get(4).getStartTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(saved.get(4).getEndTime()).isEqualTo(LocalTime.of(11, 30));
    }

    @Test
    @DisplayName("근무 삭제 - minWorkUnitMinutes=60 설정에서 90분 범위(09:00~10:30) 삭제 시 30분 슬롯 3개를 정확히 취소한다")
    void changeWorkSchedules_Delete90min_WithMin60_CancelsThreeSlots() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        // DB에 30분 슬롯 3개가 저장되어 있음 (min=60이었던 이전 버그로 저장됐든 30분 단위로 저장됐든 무관)
        WorkSchedule s1 = workSchedule(user, date, LocalTime.of(9, 0), LocalTime.of(9, 30));
        WorkSchedule s2 = workSchedule(user, date, LocalTime.of(9, 30), LocalTime.of(10, 0));
        WorkSchedule s3 = workSchedule(user, date, LocalTime.of(10, 0), LocalTime.of(10, 30));

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of(s1, s2, s3));
        // addSlots 없으므로 getRequiredSetting 불필요
        // deleteSlots 검증: setting 없으면 스킵 (loadOptionalSettings → Optional.empty)

        WorkScheduleSlotCommand deleteSlot = new WorkScheduleSlotCommand(
                date, LocalTime.of(9, 0), LocalTime.of(10, 30));
        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(), List.of(deleteSlot)));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
        verify(workSchedulesRepository, never()).saveAll(anyList());
        // 30분 단위 3슬롯 각각에 대해 isScheduleInsertable 호출 없음 (삭제 경로)
        verify(scheduleValidator, never()).isScheduleInsertable(any(), anyInt(), anyList());
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

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

    private WorkScheduleSetting setting60() {
        return WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026)
                .month(8)
                .maxConcurrentWorkers(3)
                .minWorkUnitMinutes(60)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .build();
    }

    private WorkSchedule workSchedule(User user, LocalDate date, LocalTime start, LocalTime end) {
        return WorkSchedule.builder()
                .user(user)
                .date(date)
                .startTime(start)
                .endTime(end)
                .statusCode(CodeType.WS01)
                .build();
    }
}
