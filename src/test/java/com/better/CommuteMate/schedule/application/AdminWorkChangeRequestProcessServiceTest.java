package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkChangeRequestProcessServiceTest {

    @Mock WorkChangeRequestRepository requestRepository;
    @Mock WorkChangeRequestItemRepository itemRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkplaceRepository workplaceRepository;
    @Mock ScheduleValidator scheduleValidator;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock WorkUnavailableTimeRepository unavailableTimeRepository;

    AdminWorkChangeRequestProcessService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkChangeRequestProcessService(
                requestRepository,
                itemRepository,
                scheduleRepository,
                settingRepository,
                workplaceRepository,
                scheduleValidator,
                messagingTemplate,
                unavailableTimeRepository
        );
    }

    @Test
    @DisplayName("수정 요청 승인 - 삭제 스케줄은 취소하고 추가 스케줄은 승인 상태로 생성한다")
    void approvesRequestByCancellingDeleteAndCreatingApprovedAdd() {
        WorkChangeRequest request = pendingRequest();
        WorkSchedule deletedSchedule = WorkSchedule.builder()
                .scheduleId(1L)
                .user(request.getUser())
                .date(LocalDate.of(2026, 6, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .statusCode(CodeType.WS02)
                .build();
        WorkChangeRequestItem deleteItem = item(
                request, CodeType.CR02, deletedSchedule, 15, 9, 11
        );
        WorkChangeRequestItem addItem = WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(CodeType.CR01)
                .schedule(null)
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(13, 30))
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026)
                .month(6)
                .maxConcurrentWorkers(4)
                .build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L))
                .thenReturn(List.of(deleteItem, addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findByUser_UserIdAndDateAndStartTimeAndEndTime(
                any(), any(), any(), any())).thenReturn(Optional.of(deletedSchedule));
        when(scheduleValidator.isScheduleInsertable(
                any(WorkScheduleSlotCommand.class), anyInt(), anyList()
        )).thenReturn(true);

        var response = service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L
        );

        assertThat(request.getStatusCode()).isEqualTo(CodeType.CS02);
        assertThat(request.getProcessedBy()).isEqualTo(99L);
        assertThat(deletedSchedule.getStatusCode()).isEqualTo(CodeType.WS04);
        assertThat(response.deleteSchedules).singleElement()
                .extracting(result -> result.statusCode()).isEqualTo("WS04");
        assertThat(response.addSchedules).singleElement()
                .extracting(result -> result.statusCode()).isEqualTo("WS02");
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/notifications/2"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("수정 요청 거절 - 스케줄을 변경하지 않고 거절 사유를 저장한다")
    void rejectsWithoutChangingSchedules() {
        WorkChangeRequest request = pendingRequest();
        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));

        var response = service.process(
                1L,
                new ProcessWorkChangeRequest("CS03", "정원 초과"),
                99L,
                10L
        );

        assertThat(request.getStatusCode()).isEqualTo(CodeType.CS03);
        assertThat(request.getRejectReason()).isEqualTo("정원 초과");
        assertThat(response.rejectReason).isEqualTo("정원 초과");
        assertThat(response.addSchedules).isNull();
    }

    @Test
    @DisplayName("수정 요청 처리 - 이미 처리된 요청이면 실패한다")
    void rejectsAlreadyProcessedRequest() {
        WorkChangeRequest request = pendingRequest();
        request.setStatusCode(CodeType.CS02);
        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS03", "사유"), 99L, 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 처리된 요청입니다.");
    }

    @Test
    @DisplayName("수정 요청 승인 - CR01 항목 2개(같은 월)가 예외 없이 승인되고 슬롯이 2건 생성된다")
    void approvesTwoCr01ItemsInSameMonthWithoutException() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem1 = WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(CodeType.CR01)
                .schedule(null)
                .date(LocalDate.of(2026, 6, 16))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .build();
        WorkChangeRequestItem addItem2 = WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(CodeType.CR01)
                .schedule(null)
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L))
                .thenReturn(List.of(addItem1, addItem2));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(setting));
        when(scheduleValidator.isScheduleInsertable(any(), anyInt(), anyList())).thenReturn(true);

        var response = service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L);

        assertThat(response.addSchedules).hasSize(2);
        assertThat(response.addSchedules).allMatch(r -> r.statusCode().equals("WS02"));
    }

    @Test
    @DisplayName("수정 요청 승인 - CR01 항목 2개(다른 월)에서 findForUpdate가 두 번 호출되어도 예외 없이 승인된다")
    void approvesTwoCr01ItemsAcrossDifferentMonthsWithoutException() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItemJune = WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(CodeType.CR01)
                .schedule(null)
                .date(LocalDate.of(2026, 6, 30))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .build();
        WorkChangeRequestItem addItemJuly = WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(CodeType.CR01)
                .schedule(null)
                .date(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
        WorkScheduleSetting juneSetting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();
        WorkScheduleSetting julySetting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(7).maxConcurrentWorkers(4).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L))
                .thenReturn(List.of(addItemJune, addItemJuly));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(juneSetting));
        when(settingRepository.findForUpdate(10L, 2026, 7)).thenReturn(Optional.of(julySetting));
        when(scheduleValidator.isScheduleInsertable(any(), anyInt(), anyList())).thenReturn(true);

        var response = service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L);

        assertThat(response.addSchedules).hasSize(2);
        assertThat(response.addSchedules).allMatch(r -> r.statusCode().equals("WS02"));
    }

    @Test
    @DisplayName("수정 요청 거절 - 거절 사유가 없으면 실패한다")
    void requiresRejectReason() {
        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS03", " "), 99L, 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("거절 사유를 입력해야 합니다.");
    }

    @Test
    @DisplayName("수정 요청 승인 - 최대 동시 근무 인원을 초과하면 실패한다")
    void rejectsCapacityExceededApproval() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem = item(
                request, CodeType.CR01, null, 17, 13, 15
        );
        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L)).thenReturn(List.of(addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6))
                .thenReturn(Optional.of(WorkScheduleSetting.builder()
                        .maxConcurrentWorkers(4).build()));
        when(scheduleValidator.isScheduleInsertable(any(WorkScheduleSlotCommand.class), anyInt(), anyList())).thenReturn(false);

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("해당 시간대의 최대 근무 인원을 초과했습니다.");
    }

    // ─── unavailable 시간대 승인 차단 ─────────────────────────────────────

    @Test
    @DisplayName("승인 - 부분 불가(10:00~11:00)인 날 13:00~13:30 추가는 통과한다")
    void process_SlotOutsideUnavailable_Passes() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem = item(request, CodeType.CR01, null, 17, 13, 14);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L)).thenReturn(List.of(addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(setting));
        when(unavailableTimeRepository.findBySettingAndDateBetween(
                any(), any(), any())).thenReturn(List.of(unavailable));
        when(scheduleValidator.isScheduleInsertable(any(), anyInt(), anyList())).thenReturn(true);

        var response = service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L);

        assertThat(response.addSchedules).hasSize(2); // 13:00~13:30, 13:30~14:00
    }

    @Test
    @DisplayName("승인 - 부분 불가(10:00~11:00) 날 10:00~10:30 추가는 차단된다")
    void process_SlotInsidePartialUnavailable_ThrowsConflict() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem = item(request, CodeType.CR01, null, 17, 10, 11);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L)).thenReturn(List.of(addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(setting));
        when(unavailableTimeRepository.findBySettingAndDateBetween(
                any(), any(), any())).thenReturn(List.of(unavailable));

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage("근무 불가 시간대에 신청할 수 없습니다.");
    }

    @Test
    @DisplayName("승인 - 종일 불가(MIN~MAX) 날 09:00~09:30 추가는 차단된다 (sentinel 확장 확인)")
    void process_SlotInsideAllDayUnavailable_ThrowsConflict() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem = item(request, CodeType.CR01, null, 17, 9, 10);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();
        WorkUnavailableTime allDay = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L)).thenReturn(List.of(addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(setting));
        when(unavailableTimeRepository.findBySettingAndDateBetween(
                any(), any(), any())).thenReturn(List.of(allDay));

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage("근무 불가 시간대에 신청할 수 없습니다.");
    }

    @Test
    @DisplayName("승인 - 신청 이후 관리자가 불가 설정 추가 시 승인 시점 검증으로 차단된다 (시점 문제)")
    void process_UnavailableAddedAfterRequest_BlockedAtApproval() {
        WorkChangeRequest request = pendingRequest();
        WorkChangeRequestItem addItem = item(request, CodeType.CR01, null, 17, 9, 10);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L).year(2026).month(6).maxConcurrentWorkers(4).build();
        WorkUnavailableTime laterAdded = WorkUnavailableTime.builder()
                .date(LocalDate.of(2026, 6, 17))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L)).thenReturn(List.of(addItem));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().workplaceId(1L).build()));
        when(settingRepository.findForUpdate(10L, 2026, 6)).thenReturn(Optional.of(setting));
        when(unavailableTimeRepository.findBySettingAndDateBetween(
                any(), any(), any())).thenReturn(List.of(laterAdded));

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage("근무 불가 시간대에 신청할 수 없습니다.");
    }

    private WorkChangeRequest pendingRequest() {
        User user = User.builder()
                .userId(2L)
                .organizationId(10L)
                .name("김길동")
                .build();
        return WorkChangeRequest.builder()
                .requestId(1L)
                .user(user)
                .statusCode(CodeType.CS01)
                .reason("변경 요청")
                .build();
    }

    private WorkChangeRequestItem item(
            WorkChangeRequest request,
            CodeType type,
            WorkSchedule schedule,
            int day,
            int startHour,
            int endHour
    ) {
        return WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(type)
                .schedule(schedule)
                .date(LocalDate.of(2026, 6, day))
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .build();
    }
}
