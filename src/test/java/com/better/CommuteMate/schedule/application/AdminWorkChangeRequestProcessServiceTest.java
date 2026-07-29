package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
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
                messagingTemplate
        );
    }

    @Test
    @DisplayName("수정 요청 승인 - 삭제 스케줄은 취소하고 추가 스케줄은 승인 상태로 생성한다")
    void approvesRequestByCancellingDeleteAndCreatingApprovedAdd() {
        WorkChangeRequest request = pendingRequest();
        WorkSchedule deletedSchedule = WorkSchedule.builder()
                .scheduleId("delete-id")
                .user(request.getUser())
                .date(LocalDate.of(2026, 6, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .statusCode(CodeType.WS02)
                .build();
        WorkChangeRequestItem deleteItem = item(
                request, CodeType.CR02, deletedSchedule, 15, 9, 11
        );
        WorkChangeRequestItem addItem = item(
                request, CodeType.CR01, null, 17, 13, 15
        );
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId("10")
                .year(2026)
                .month(6)
                .maxConcurrentWorkers(4)
                .build();

        when(requestRepository.findForProcessing(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequest_RequestId(1L))
                .thenReturn(List.of(deleteItem, addItem));
        when(workplaceRepository.findFirstByOrganizationId("10"))
                .thenReturn(Optional.of(Workplace.builder().workplaceId("workplace").build()));
        when(settingRepository.findForUpdate("10", 2026, 6))
                .thenReturn(Optional.of(setting));
        when(scheduleValidator.isScheduleInsertable(
                any(WorkScheduleSlotCommand.class), any(WorkScheduleSetting.class)
        )).thenReturn(true);
        when(scheduleRepository.saveAndFlush(any(WorkSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L
        );

        assertThat(request.getStatusCode()).isEqualTo(CodeType.CS02);
        assertThat(request.getProcessedBy()).isEqualTo(99L);
        assertThat(deletedSchedule.getStatusCode()).isEqualTo(CodeType.WS04);
        assertThat(addItem.getSchedule()).isNotNull();
        assertThat(addItem.getSchedule().getStatusCode()).isEqualTo(CodeType.WS02);
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
        when(workplaceRepository.findFirstByOrganizationId("10"))
                .thenReturn(Optional.of(Workplace.builder().workplaceId("workplace").build()));
        when(settingRepository.findForUpdate("10", 2026, 6))
                .thenReturn(Optional.of(WorkScheduleSetting.builder()
                        .maxConcurrentWorkers(4).build()));
        when(scheduleValidator.isScheduleInsertable(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.process(
                1L, new ProcessWorkChangeRequest("CS02", null), 99L, 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("해당 시간대의 최대 근무 인원을 초과했습니다.");
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
