package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
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
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleApplyPeriodResponse;
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

    // ── edit 합산 검증 케이스 ──────────────────────────────────────────

    @Test
    @DisplayName("수정 신청 - 기존 DB 슬롯(09:00~10:00 WS02)에 10:00~10:30 추가 → 합산 90분 → min=60 통과")
    void submitEditRequest_WithExistingDb_Plus30minAdd_Combined90min_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        WorkSchedule db1 = WorkSchedule.builder()
                .user(user).date(date)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .statusCode(CodeType.WS02).build();
        WorkSchedule db2 = WorkSchedule.builder()
                .user(user).date(date)
                .startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0))
                .statusCode(CodeType.WS02).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting60()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                eq(1L), eq(date), eq(date), anyList())).thenReturn(List.of(db1, db2));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(workChangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(date, LocalTime.of(10, 0), LocalTime.of(10, 30))),
                "사유"
        );

        assertThat(scheduleService.submitEditRequest(1L, request)).isNotNull();
    }

    @Test
    @DisplayName("수정 신청 - 기존 DB 슬롯(09:00~09:30 WS01)에 09:30~10:00 추가 → 합산 60분 → min=60 통과")
    void submitEditRequest_WithExistingWS01_AdjacentAdd_Combined60min_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting60()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                eq(1L), eq(date), eq(date), anyList()))
                .thenReturn(List.of(workSchedule(user, date, LocalTime.of(9, 0), LocalTime.of(9, 30))));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(workChangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(date, LocalTime.of(9, 30), LocalTime.of(10, 0))),
                "사유"
        );

        assertThat(scheduleService.submitEditRequest(1L, request)).isNotNull();
    }

    @Test
    @DisplayName("수정 신청 - 기존 09:00~10:00 중 09:30~10:00 삭제 → 남는 09:00~09:30(30분) → min=60 반려")
    void submitEditRequest_DeleteLeaves30min_WithMin60_ThrowsInvalidSlotDuration() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting60()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                eq(1L), eq(date), eq(date), anyList())).thenReturn(List.of(
                workSchedule(user, date, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                workSchedule(user, date, LocalTime.of(9, 30), LocalTime.of(10, 0))));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(new WorkScheduleEditRequest.Slot(date, LocalTime.of(9, 30), LocalTime.of(10, 0))),
                List.of(),
                "사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("수정 신청 - 기존 09:00~09:30 있을 때 끊긴 10:00~10:30 추가 → 각 구간 30분, 비연속 → min=60 반려")
    void submitEditRequest_WithExisting_DisconnectedAdd_EachUnder60min_ThrowsInvalidSlotDuration() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting60()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                eq(1L), eq(date), eq(date), anyList()))
                .thenReturn(List.of(workSchedule(user, date, LocalTime.of(9, 0), LocalTime.of(9, 30))));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(date, LocalTime.of(10, 0), LocalTime.of(10, 30))),
                "사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("수정 신청 - WS03·WS04 슬롯은 ACTIVE 조회(WS01+WS02)에서 제외 → 30분 추가 단독으로 min=60 반려")
    void submitEditRequest_InactiveDbSlotsExcluded_30minAddFails() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        LocalDate date = LocalDate.of(2026, 8, 10);

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting60()));
        // WS01+WS02 조회 결과 빈 리스트 (WS03/WS04만 존재하는 상황을 모사)
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                eq(1L), eq(date), eq(date), anyList())).thenReturn(List.of());

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(date, LocalTime.of(9, 0), LocalTime.of(9, 30))),
                "사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class);
    }

    // ── unavailable 신청 차단 (apply) ─────────────────────────────────

    @Test
    @DisplayName("근무 신청 - 부분 불가(10:00~11:00) 시간대 신청은 failure에 담긴다")
    void changeWorkSchedules_PartialUnavailable_SlotGoesToFailure() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(unavailable));

        // 10:00~11:00 (불가 시간대 그대로 신청)
        WorkScheduleSlotCommand badSlot = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(11, 0));
        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(badSlot), List.of()));

        assertThat(result.success()).isEmpty();
        assertThat(result.failure()).hasSize(1);
        verify(workSchedulesRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("근무 신청 - 종일 불가(MIN~MAX) 날 09:00~10:00 신청은 failure에 담긴다")
    void changeWorkSchedules_AllDayUnavailable_SlotGoesToFailure() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();
        WorkUnavailableTime allDay = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(allDay));

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of()));

        assertThat(result.success()).isEmpty();
        assertThat(result.failure()).hasSize(1);
        verify(workSchedulesRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("근무 신청 - 불가 경계: 09:30~10:00은 통과, 10:00~10:30은 차단 (동등비교 경계)")
    void changeWorkSchedules_BoundaryUnavailable_PassesBeforeBlocksAfter() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(unavailable));
        when(scheduleValidator.isScheduleInsertable(any(), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        // 09:30~10:00 → 불가 시간대 외 → 통과
        WorkScheduleSlotCommand before = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 30), LocalTime.of(10, 0));
        // 10:00~10:30 → 불가 시간대 경계 내 → 차단
        WorkScheduleSlotCommand inside = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(10, 30));

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(before, inside), List.of()));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).hasSize(1);
    }

    @Test
    @DisplayName("근무 신청 - unavailable 없으면 정상 신청 통과 (회귀)")
    void changeWorkSchedules_NoUnavailable_Passes() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = setting();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting);
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(scheduleValidator.isScheduleInsertable(any(), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        WorkScheduleChangeResultCommand result = scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of()));

        assertThat(result.success()).hasSize(1);
        assertThat(result.failure()).isEmpty();
    }

    // ── unavailable 신청 차단 (edit 요청) ──────────────────────────────

    @Test
    @DisplayName("수정 신청 - 부분 불가(10:00~11:00) 시간대 add 요청은 throw된다")
    void submitEditRequest_PartialUnavailable_ThrowsConflict() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.of(setting()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(unavailable));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(11, 0))),
                "사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("근무 불가 시간대에 신청할 수 없습니다.");
    }

    @Test
    @DisplayName("수정 신청 - 종일 불가(MIN~MAX) 날 09:00~10:00 add 요청은 throw된다")
    void submitEditRequest_AllDayUnavailable_ThrowsConflict() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkUnavailableTime allDay = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.of(setting()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workUnavailableTimeRepository.findBySettingAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(allDay));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0))),
                "사유"
        );

        assertThatThrownBy(() -> scheduleService.submitEditRequest(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("근무 불가 시간대에 신청할 수 없습니다.");
    }

    @Test
    @DisplayName("수정 신청 - setting 없으면 unavailable 검증 skip (정상 통과)")
    void submitEditRequest_NoSetting_SkipsUnavailableCheck() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getSetting(10L, 2026, 8))
                .thenReturn(Optional.empty());
        when(workChangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkScheduleEditRequest request = new WorkScheduleEditRequest(
                List.of(),
                List.of(new WorkScheduleEditRequest.Slot(
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0))),
                "사유"
        );

        assertThat(scheduleService.submitEditRequest(1L, request)).isNotNull();
    }

    // ── 신청 기간 검증 ────────────────────────────────────────────────

    @Test
    @DisplayName("근무 신청 - 신청 기간 종료(applyEndAt 경과) 후 신청 시 APPLY_PERIOD_NOT_ACTIVE 예외")
    void changeWorkSchedules_AfterApplyEndAt_ThrowsApplyPeriodNotActive() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(settingExpired());

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 신청 기간이 아닙니다.");
    }

    @Test
    @DisplayName("근무 신청 - applyStartAt 이전에 신청 시 APPLY_PERIOD_NOT_ACTIVE 예외")
    void changeWorkSchedules_BeforeApplyStartAt_ThrowsApplyPeriodNotActive() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(settingFuture());

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 신청 기간이 아닙니다.");
    }

    @Test
    @DisplayName("근무 신청 - applyEnabled=false 이면 기간 내여도 APPLY_PERIOD_NOT_ACTIVE 예외")
    void changeWorkSchedules_ApplyDisabled_ThrowsApplyPeriodNotActive() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(settingDisabled());

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot), List.of())))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 신청 기간이 아닙니다.");
    }

    @Test
    @DisplayName("근무 신청 - 신청 기간 내 저장 시 상태코드가 항상 WS02이다")
    void changeWorkSchedules_InPeriod_SavesAsWS02() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting());
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(true);
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().organizationId(10L).name("본사").build()));

        scheduleService.changeWorkSchedules(new WorkScheduleChangeCommand(1L, List.of(slot), List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(workSchedulesRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allMatch(s -> s.getStatusCode() == CodeType.WS02);
    }

    @Test
    @DisplayName("근무 신청 - 여러 월 중 한 달이 기간 밖이면 요청 전체가 APPLY_PERIOD_NOT_ACTIVE로 거부된다")
    void changeWorkSchedules_MultiMonth_OneExpired_ThrowsApplyPeriodNotActive() {
        User user = User.builder().userId(1L).organizationId(10L).build();

        WorkScheduleSlotCommand slotSep = new WorkScheduleSlotCommand(
                LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(11, 0));

        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 8)).thenReturn(setting());
        when(workScheduleSettingService.getRequiredSetting(10L, 2026, 9)).thenReturn(settingExpired());

        assertThatThrownBy(() -> scheduleService.changeWorkSchedules(
                new WorkScheduleChangeCommand(1L, List.of(slot, slotSep), List.of())))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 신청 기간이 아닙니다.");

        verify(workSchedulesRepository, never()).saveAll(anyList());
    }

    // ── 근로 신청 기간 조회 ─────────────────────────────────────────────

    @Test
    @DisplayName("근로 신청 기간 조회 - 신청 기간 내이면 isApplyAvailable=true, isEditAvailable=false")
    void getApplyPeriod_WithinPeriod_ApplyAvailableEditNotAvailable() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(3);
        LocalDate end = today.plusDays(3);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(start.atStartOfDay())
                .applyEndAt(end.atStartOfDay())
                .build();
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting));

        WorkScheduleApplyPeriodResponse response = scheduleService.getApplyPeriod(10L, 2026, 8);

        assertThat(response.getApplyStartDate()).isEqualTo(start);
        assertThat(response.getApplyEndDate()).isEqualTo(end);
        assertThat(response.getIsApplyAvailable()).isTrue();
        assertThat(response.getIsEditAvailable()).isFalse();
    }

    @Test
    @DisplayName("근로 신청 기간 조회 - 종료일 당일(applyEndAt이 종료일 00:00으로 저장)에도 신청 가능해야 한다 (경계 처리)")
    void getApplyPeriod_OnEndDate_StillApplyAvailable() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(9);
        // 신청 종료 시각이 종료일 자정(00:00)으로 저장된 경우를 재현한다.
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(start.atStartOfDay())
                .applyEndAt(today.atStartOfDay())
                .build();
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting));

        WorkScheduleApplyPeriodResponse response = scheduleService.getApplyPeriod(10L, 2026, 8);

        assertThat(response.getIsApplyAvailable()).isTrue();
        assertThat(response.getIsEditAvailable()).isFalse();
    }

    @Test
    @DisplayName("근로 신청 기간 조회 - 신청 기간 종료 후에는 isApplyAvailable=false, isEditAvailable=true")
    void getApplyPeriod_AfterPeriod_ApplyNotAvailableEditAvailable() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(10);
        LocalDate end = today.minusDays(1);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(start.atStartOfDay())
                .applyEndAt(end.atStartOfDay())
                .build();
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting));

        WorkScheduleApplyPeriodResponse response = scheduleService.getApplyPeriod(10L, 2026, 8);

        assertThat(response.getIsApplyAvailable()).isFalse();
        assertThat(response.getIsEditAvailable()).isTrue();
    }

    @Test
    @DisplayName("근로 신청 기간 조회 - applyEnabled=false이면 기간 내라도 isApplyAvailable=false")
    void getApplyPeriod_ApplyDisabled_ApplyNotAvailable() {
        LocalDate today = LocalDate.now();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(8)
                .applyStartAt(today.minusDays(3).atStartOfDay())
                .applyEndAt(today.plusDays(3).atStartOfDay())
                .applyEnabled(false)
                .build();
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.of(setting));

        WorkScheduleApplyPeriodResponse response = scheduleService.getApplyPeriod(10L, 2026, 8);

        assertThat(response.getIsApplyAvailable()).isFalse();
        assertThat(response.getIsEditAvailable()).isTrue();
    }

    @Test
    @DisplayName("근로 신청 기간 조회 - 해당 월 설정이 없으면 날짜는 null, isApplyAvailable=false, isEditAvailable=true")
    void getApplyPeriod_NoSetting_DefaultsToUnavailableApplyEditableTrue() {
        when(workScheduleSettingService.getSetting(10L, 2026, 8)).thenReturn(Optional.empty());

        WorkScheduleApplyPeriodResponse response = scheduleService.getApplyPeriod(10L, 2026, 8);

        assertThat(response.getApplyStartDate()).isNull();
        assertThat(response.getApplyEndDate()).isNull();
        assertThat(response.getIsApplyAvailable()).isFalse();
        assertThat(response.getIsEditAvailable()).isTrue();
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

    private WorkScheduleSetting settingExpired() {
        return WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026).month(8)
                .maxConcurrentWorkers(3)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2020, 12, 31, 23, 59))
                .build();
    }

    private WorkScheduleSetting settingFuture() {
        return WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026).month(8)
                .maxConcurrentWorkers(3)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2030, 12, 31, 23, 59))
                .build();
    }

    private WorkScheduleSetting settingDisabled() {
        return WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026).month(8)
                .maxConcurrentWorkers(3)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .applyEnabled(false)
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
