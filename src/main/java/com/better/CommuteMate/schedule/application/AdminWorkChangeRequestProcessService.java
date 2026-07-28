package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeResponse;
import com.better.CommuteMate.schedule.controller.dtos.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminWorkChangeRequestProcessService {

    private final WorkChangeRequestRepository requestRepository;
    private final WorkChangeRequestItemRepository itemRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkScheduleSettingRepository settingRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ScheduleValidator scheduleValidator;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ProcessWorkChangeResponse process(
            Long requestId,
            ProcessWorkChangeRequest command,
            Long adminId,
            Long organizationId
    ) {
        CodeType targetStatus = validateCommand(command);
        WorkChangeRequest request = requestRepository.findForProcessing(requestId)
                .filter(found -> found.getUser().getOrganizationId().equals(organizationId))
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.CHANGE_REQUEST_NOT_FOUND));

        if (request.getStatusCode() != CodeType.CS01) {
            throw CustomException.of(ScheduleErrorCode.CHANGE_REQUEST_ALREADY_PROCESSED);
        }

        LocalDateTime processedAt = LocalDateTime.now();
        request.setStatusCode(targetStatus);
        request.setProcessedAt(processedAt);
        request.setProcessedBy(adminId);
        request.setUpdatedBy(adminId);

        if (targetStatus == CodeType.CS03) {
            String rejectReason = command.rejectReason().trim();
            request.setRejectReason(rejectReason);
            sendNotification(request, false);
            return new ProcessWorkChangeResponse(
                    requestId, targetStatus.name(), processedAt,
                    rejectReason, null, null
            );
        }

        request.setRejectReason(null);
        List<WorkChangeRequestItem> items =
                itemRepository.findAllByRequest_RequestId(requestId);
        List<ProcessWorkChangeResponse.ScheduleResult> deleted = new ArrayList<>();
        List<ProcessWorkChangeResponse.ScheduleResult> added = new ArrayList<>();

        for (WorkChangeRequestItem item : items) {
            if (item.getChangeTypeCode() != CodeType.CR02) {
                continue;
            }
            WorkSchedule schedule = item.getSchedule();
            if (schedule == null) {
                schedule = scheduleRepository
                        .findByUser_UserIdAndDateAndStartTimeAndEndTime(
                                request.getUser().getUserId(),
                                item.getDate(),
                                item.getStartTime(),
                                item.getEndTime()
                        )
                        .orElseThrow(() -> CustomException.of(
                                ScheduleErrorCode.DELETE_SCHEDULE_NOT_FOUND
                        ));
                item.linkSchedule(schedule);
            }
            schedule.cancel(String.valueOf(adminId));
            deleted.add(toResult(schedule));
        }
        scheduleRepository.flush();

        Workplace workplace = workplaceRepository
                .findFirstByOrganizationId(String.valueOf(organizationId))
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE));

        for (WorkChangeRequestItem item : items) {
            if (item.getChangeTypeCode() != CodeType.CR01) {
                continue;
            }
            YearMonth month = YearMonth.from(item.getDate());
            WorkScheduleSetting setting = settingRepository.findForUpdate(
                            String.valueOf(organizationId),
                            month.getYear(),
                            month.getMonthValue()
                    )
                    .orElseThrow(() -> CustomException.of(
                            ScheduleErrorCode.ADMIN_SCHEDULE_SETTING_NOT_FOUND
                    ));
            WorkScheduleSlotCommand slot = new WorkScheduleSlotCommand(
                    item.getDate(), item.getStartTime(), item.getEndTime()
            );
            if (!scheduleValidator.isScheduleInsertable(slot, setting)) {
                throw CustomException.of(ScheduleErrorCode.CHANGE_REQUEST_CAPACITY_EXCEEDED);
            }

            WorkSchedule schedule = WorkSchedule.builder()
                    .user(request.getUser())
                    .setting(setting)
                    .workplace(workplace)
                    .date(item.getDate())
                    .startTime(item.getStartTime())
                    .endTime(item.getEndTime())
                    .statusCode(CodeType.WS02)
                    .createdRequestId(String.valueOf(requestId))
                    .createdBy(String.valueOf(adminId))
                    .updatedBy(String.valueOf(adminId))
                    .build();
            scheduleRepository.saveAndFlush(schedule);
            item.linkSchedule(schedule);
            added.add(toResult(schedule));
        }

        sendNotification(request, true);
        return new ProcessWorkChangeResponse(
                requestId, targetStatus.name(), processedAt,
                null, deleted, added
        );
    }

    private CodeType validateCommand(ProcessWorkChangeRequest command) {
        if (command == null || command.statusCode() == null) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_PROCESS_STATUS);
        }
        CodeType status;
        try {
            status = CodeType.valueOf(
                    command.statusCode().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_PROCESS_STATUS);
        }
        if (status != CodeType.CS02 && status != CodeType.CS03) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_PROCESS_STATUS);
        }
        if (status == CodeType.CS03
                && (command.rejectReason() == null || command.rejectReason().isBlank())) {
            throw CustomException.of(ScheduleErrorCode.CHANGE_REQUEST_REJECT_REASON_REQUIRED);
        }
        return status;
    }

    private ProcessWorkChangeResponse.ScheduleResult toResult(WorkSchedule schedule) {
        return new ProcessWorkChangeResponse.ScheduleResult(
                schedule.getScheduleId(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStatusCode().name()
        );
    }

    private void sendNotification(WorkChangeRequest request, boolean approved) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(approved ? "SCHEDULE_APPROVED" : "SCHEDULE_REJECTED")
                .message(approved
                        ? "근로시간 수정 요청이 승인되었습니다."
                        : "근로시간 수정 요청이 거절되었습니다.")
                .data(String.valueOf(request.getRequestId()))
                .build();
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + request.getUser().getUserId(),
                notification
        );
    }
}
