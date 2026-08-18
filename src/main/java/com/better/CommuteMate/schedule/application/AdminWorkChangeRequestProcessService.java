package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.schedule.application.WorkSlotUtils.SlotKey;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminWorkChangeRequestProcessService {

    private static final List<CodeType> ACTIVE_STATUSES = List.of(CodeType.WS01, CodeType.WS02);
    private static final int SLOT_MINUTES = 30;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME   = LocalTime.of(18, 0);

    private final WorkChangeRequestRepository requestRepository;
    private final WorkChangeRequestItemRepository itemRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkScheduleSettingRepository settingRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ScheduleValidator scheduleValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final WorkUnavailableTimeRepository unavailableTimeRepository;

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

        // === CR02: 교체 대상을 단위 슬롯 단위로 모두 취소한다.
        // 취소를 먼저 완료해야 이후 CR01 정원 계산이 정확해진다.
        for (WorkChangeRequestItem item : items) {
            if (item.getChangeTypeCode() != CodeType.CR02) {
                continue;
            }
            YearMonth month = YearMonth.from(item.getDate());
            WorkScheduleSetting setting = settingRepository.findForUpdate(
                            organizationId, month.getYear(), month.getMonthValue())
                    .orElseThrow(() -> CustomException.of(
                            ScheduleErrorCode.ADMIN_SCHEDULE_SETTING_NOT_FOUND));

            List<WorkScheduleSlotCommand> unitSlots = WorkSlotUtils.splitIntoUnitSlots(
                    item.getDate(), item.getStartTime(), item.getEndTime(), SLOT_MINUTES);

            for (WorkScheduleSlotCommand unitSlot : unitSlots) {
                Optional<WorkSchedule> scheduleOpt = scheduleRepository
                        .findFirstByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeInOrderByUpdatedAtDesc(
                                request.getUser().getUserId(),
                                unitSlot.date(), unitSlot.start(), unitSlot.end(), ACTIVE_STATUSES);

                if (scheduleOpt.isEmpty()) {
                    continue;
                }
                WorkSchedule schedule = scheduleOpt.get();
                schedule.cancel(String.valueOf(adminId));
                deleted.add(toResult(schedule));
            }
        }
        // 취소 상태를 DB에 반영해 이후 정원 조회가 올바른 값을 반환하도록 한다.
        scheduleRepository.flush();

        Workplace workplace = workplaceRepository
                .findFirstByOrganizationId(organizationId)
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE));

        // === CR01: 원본 범위를 단위 슬롯으로 분할해 정원을 확인하고 일괄 저장한다.
        // daySchedulesMap: 날짜별 당일 근무 목록. 이미 추가한 슬롯도 여기에 넣어
        // 다음 단위 슬롯의 정원 검사에 반영한다 (saveAll 전에 DB flush 없이도 정확한 검사 가능).
        Map<LocalDate, List<WorkSchedule>> daySchedulesMap = new HashMap<>();
        Map<LocalDate, Set<SlotKey>> unavailableByDate = new HashMap<>();
        List<WorkSchedule> toSave = new ArrayList<>();

        for (WorkChangeRequestItem item : items) {
            if (item.getChangeTypeCode() != CodeType.CR01) {
                continue;
            }
            YearMonth month = YearMonth.from(item.getDate());
            WorkScheduleSetting setting = settingRepository.findForUpdate(
                            organizationId, month.getYear(), month.getMonthValue())
                    .orElseThrow(() -> CustomException.of(
                            ScheduleErrorCode.ADMIN_SCHEDULE_SETTING_NOT_FOUND));

            List<WorkScheduleSlotCommand> unitSlots = WorkSlotUtils.splitIntoUnitSlots(
                    item.getDate(), item.getStartTime(), item.getEndTime(), SLOT_MINUTES);

            List<WorkSchedule> dayList = daySchedulesMap.computeIfAbsent(
                    item.getDate(), d -> new ArrayList<>(scheduleRepository.findAllByDate(d)));

            Set<SlotKey> unavailableSlots = unavailableByDate.computeIfAbsent(
                    item.getDate(), d -> WorkSlotUtils.buildUnavailableSlotKeys(
                            unavailableTimeRepository.findBySettingAndDateBetween(setting, d, d),
                            WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES));

            for (WorkScheduleSlotCommand unitSlot : unitSlots) {
                SlotKey key = new SlotKey(unitSlot.date(), unitSlot.start(), unitSlot.end());
                if (unavailableSlots.contains(key)) {
                    throw CustomException.of(ScheduleErrorCode.UNAVAILABLE_TIME_CONFLICT);
                }
                if (!scheduleValidator.isScheduleInsertable(
                        unitSlot, setting.getMaxConcurrentWorkers(), dayList)) {
                    throw CustomException.of(ScheduleErrorCode.CHANGE_REQUEST_CAPACITY_EXCEEDED);
                }
                WorkSchedule schedule = WorkSchedule.builder()
                        .user(request.getUser())
                        .setting(setting)
                        .workplace(workplace)
                        .date(unitSlot.date())
                        .startTime(unitSlot.start())
                        .endTime(unitSlot.end())
                        .statusCode(CodeType.WS02)
                        .createdRequestId(String.valueOf(requestId))
                        .createdBy(String.valueOf(adminId))
                        .updatedBy(String.valueOf(adminId))
                        .build();
                // dayList에 포함시켜 다음 슬롯 정원 검사에 반영한다.
                dayList.add(schedule);
                toSave.add(schedule);
            }
        }

        if (!toSave.isEmpty()) {
            scheduleRepository.saveAll(toSave);
        }
        toSave.forEach(s -> added.add(toResult(s)));

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
