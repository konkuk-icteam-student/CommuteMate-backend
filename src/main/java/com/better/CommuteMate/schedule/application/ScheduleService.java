package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.MonthlyWorkTimeExceededException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import com.better.CommuteMate.schedule.controller.dtos.ScheduleUpdateMessage;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeResponseDetail;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ScheduleValidator scheduleValidator;
    private final WorkScheduleSettingService workScheduleSettingService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int MONTHLY_LIMIT_HOURS = 27;
    private static final int MONTHLY_LIMIT_MINUTES = MONTHLY_LIMIT_HOURS * 60;

    /**
     * 근무 일정 변경사항을 반영합니다.
     * addSlots는 새 일정을 추가하고, deleteSlots는 기존 일정을 취소합니다.
     */
    @Transactional
    public WorkScheduleChangeResultCommand changeWorkSchedules(
            WorkScheduleChangeCommand command
    ) {
        validateChangeCommand(command);

        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        List<WorkScheduleSlotCommand> addSlots = command.addSlots();
        List<WorkScheduleSlotCommand> deleteSlots = command.deleteSlots();

        validateSlots(addSlots);
        validateSlots(deleteSlots);

        validateMonthlyLimit(command.userId(), addSlots, deleteSlots);

        List<WorkScheduleChangeResponseDetail.Slot> success = new ArrayList<>();
        List<WorkScheduleChangeResponseDetail.Slot> failure = new ArrayList<>();
        List<ScheduleChange> changes = new ArrayList<>();

        for (WorkScheduleSlotCommand slot : deleteSlots) {
            deleteSlot(command.userId(), slot, success, failure, changes);
        }

        for (WorkScheduleSlotCommand slot : addSlots) {
            addSlot(user, slot, success, failure, changes);
        }

        broadcastScheduleUpdate(changes);

        return WorkScheduleChangeResultCommand.of(success, failure);
    }

    /**
     * 변경 요청이 비어있는지 검증합니다.
     */
    private void validateChangeCommand(WorkScheduleChangeCommand command) {
        if (command == null || command.isEmpty()) {
            throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
        }
    }

    /**
     * 요청 슬롯의 날짜/시간 값과 30분 단위를 검증합니다.
     */
    private void validateSlots(List<WorkScheduleSlotCommand> slots) {
        for (WorkScheduleSlotCommand slot : slots) {
            if (slot.date() == null || slot.start() == null || slot.end() == null) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }

            if (!slot.start().isBefore(slot.end())) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }

            if (!isThirtyMinuteUnit(slot) || !isValidWorkUnit(slot)) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }

            scheduleValidator.validateMinWorkTime(slot);
        }
    }

    /**
     * 시작/종료 시간이 30분 단위인지 확인합니다.
     */
    private boolean isThirtyMinuteUnit(WorkScheduleSlotCommand slot) {
        return (slot.start().getMinute() == 0 || slot.start().getMinute() == 30)
                && (slot.end().getMinute() == 0 || slot.end().getMinute() == 30);
    }

    /**
     * 근무 시간이 30분 단위로 나누어 떨어지는지 확인합니다.
     */
    private boolean isValidWorkUnit(WorkScheduleSlotCommand slot) {
        long minutes = Duration.between(slot.start(), slot.end()).toMinutes();
        return minutes % 30 == 0;
    }

    /**
     * 월 최대 근무 시간 초과 여부를 검증합니다.
     * deleteSlots 반영 후 addSlots를 추가했을 때의 시간을 기준으로 계산합니다.
     */
    private void validateMonthlyLimit(
            Long userId,
            List<WorkScheduleSlotCommand> addSlots,
            List<WorkScheduleSlotCommand> deleteSlots
    ) {
        if (addSlots.isEmpty()) {
            return;
        }

        YearMonth targetMonth = YearMonth.from(addSlots.get(0).date());

        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<WorkSchedule> currentSchedules =
                workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId,
                        monthStart,
                        monthEnd,
                        CodeType.WS04
                );

        long currentMinutes = currentSchedules.stream()
                .mapToLong(schedule ->
                        Duration.between(
                                schedule.getStartTime(),
                                schedule.getEndTime()
                        ).toMinutes()
                )
                .sum();

        long deleteMinutes = deleteSlots.stream()
                .filter(slot -> YearMonth.from(slot.date()).equals(targetMonth))
                .mapToLong(slot -> Duration.between(slot.start(), slot.end()).toMinutes())
                .sum();

        long addMinutes = addSlots.stream()
                .filter(slot -> YearMonth.from(slot.date()).equals(targetMonth))
                .mapToLong(slot -> Duration.between(slot.start(), slot.end()).toMinutes())
                .sum();

        long requestedMinutes = currentMinutes - deleteMinutes + addMinutes;

        if (requestedMinutes > MONTHLY_LIMIT_MINUTES) {
            throw new MonthlyWorkTimeExceededException(
                    MONTHLY_LIMIT_HOURS,
                    (int) Math.ceil(requestedMinutes / 60.0)
            );
        }
    }

    /**
     * 삭제 요청 슬롯을 처리합니다.
     * 일치하는 일정이 있으면 취소 처리하고, 없으면 실패 목록에 추가합니다.
     */
    private void deleteSlot(
            Long userId,
            WorkScheduleSlotCommand slot,
            List<WorkScheduleChangeResponseDetail.Slot> success,
            List<WorkScheduleChangeResponseDetail.Slot> failure,
            List<ScheduleChange> changes
    ) {
        Optional<WorkSchedule> scheduleOptional =
                workSchedulesRepository.findByUser_UserIdAndDateAndStartTimeAndEndTime(
                        userId,
                        slot.date(),
                        slot.start(),
                        slot.end()
                );

        if (scheduleOptional.isEmpty()) {
            failure.add(toResponseSlot(slot));
            return;
        }

        WorkSchedule schedule = scheduleOptional.get();

        if (schedule.getStatusCode().equals(CodeType.WS04)) {
            failure.add(toResponseSlot(slot));
            return;
        }

        schedule.cancel(String.valueOf(userId));

        success.add(toResponseSlot(slot));
        changes.add(new ScheduleChange(
                false,
                slot.startDateTime(),
                slot.endDateTime()
        ));
    }

    /**
     * 추가 요청 슬롯을 처리합니다.
     * 동일한 일정이 이미 있거나 동시 근무 제한을 초과하면 실패 목록에 추가합니다.
     */
    private void addSlot(
            User user,
            WorkScheduleSlotCommand slot,
            List<WorkScheduleChangeResponseDetail.Slot> success,
            List<WorkScheduleChangeResponseDetail.Slot> failure,
            List<ScheduleChange> changes
    ) {
        boolean exists =
                workSchedulesRepository.existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeNot(
                        user.getUserId(),
                        slot.date(),
                        slot.start(),
                        slot.end(),
                        CodeType.WS04
                );

        if (exists) {
            failure.add(toResponseSlot(slot));
            return;
        }

        WorkScheduleSetting setting = workScheduleSettingService.getRequiredSetting(
                String.valueOf(user.getOrganizationId()),
                slot.date().getYear(),
                slot.date().getMonthValue()
        );

        if (!scheduleValidator.isScheduleInsertable(slot, setting)) {
            failure.add(toResponseSlot(slot));
            return;
        }

        CodeType statusCode = setting.isApplyPeriod(LocalDateTime.now())
                ? CodeType.WS02
                : CodeType.WS01;

        WorkSchedule workSchedule = WorkSchedule.builder()
                .user(user)
                .setting(setting)
                .workplace(resolveWorkplace(user))
                .date(slot.date())
                .startTime(slot.start())
                .endTime(slot.end())
                .statusCode(statusCode)
                .createdBy(String.valueOf(user.getUserId()))
                .updatedBy(String.valueOf(user.getUserId()))
                .build();

        workSchedulesRepository.save(workSchedule);

        success.add(toResponseSlot(slot));

        if (statusCode.equals(CodeType.WS02)) {
            changes.add(new ScheduleChange(
                    true,
                    slot.startDateTime(),
                    slot.endDateTime()
            ));
        }
    }

    /**
     * User 기준으로 근무지를 조회합니다.
     * User 엔티티 구조에 맞게 이 부분만 수정하면 됩니다.
     */
    private Workplace resolveWorkplace(User user) {
        return workplaceRepository.findFirstByOrganizationId(String.valueOf(user.getOrganizationId()))
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE));
    }

    /**
     * Application Slot Command를 응답 Slot으로 변환합니다.
     */
    private WorkScheduleChangeResponseDetail.Slot toResponseSlot(
            WorkScheduleSlotCommand slot
    ) {
        return new WorkScheduleChangeResponseDetail.Slot(
                slot.startDateTime(),
                slot.endDateTime()
        );
    }

    /**
     * 특정 사용자의 연/월별 근무 일정 조회
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> getWorkSchedules(
            Long userId,
            Integer year,
            Integer month
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);

        return workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth(),
                        CodeType.WS04
                )
                .stream()
                .map(WorkScheduleResponse::from)
                .toList();
    }

    /**
     * 특정 사용자의 연/월별 근무 이력 조회
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleHistoryResponse> getWorkScheduleHistory(
            Long userId,
            Integer year,
            Integer month
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);

        List<WorkSchedule> schedules =
                workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth(),
                        CodeType.WS04
                );

        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();

        for (WorkSchedule schedule : schedules) {
            List<WorkAttendance> attendances =
                    workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

            Optional<WorkAttendance> checkIn = attendances.stream()
                    .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                    .findFirst();

            Optional<WorkAttendance> checkOut = attendances.stream()
                    .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                    .findFirst();

            LocalDateTime actualStart = checkIn
                    .map(WorkAttendance::getCheckTime)
                    .orElse(null);

            LocalDateTime actualEnd = checkOut
                    .map(WorkAttendance::getCheckTime)
                    .orElse(null);

            Long duration = null;

            if (actualStart != null && actualEnd != null) {
                duration = Duration.between(actualStart, actualEnd).toMinutes();
            }

            historyList.add(WorkScheduleHistoryResponse.builder()
                    .id(schedule.getScheduleId())
                    .start(LocalDateTime.of(schedule.getDate(), schedule.getStartTime()))
                    .end(LocalDateTime.of(schedule.getDate(), schedule.getEndTime()))
                    .status(schedule.getStatusCode())
                    .actualStart(actualStart)
                    .actualEnd(actualEnd)
                    .workDurationMinutes(duration)
                    .build());
        }

        historyList.sort(Comparator.comparing(WorkScheduleHistoryResponse::getStart));

        return historyList;
    }

    /**
     * 특정 근무 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public WorkScheduleResponse getWorkSchedule(
            Long userId,
            String scheduleId
    ) {
        WorkSchedule schedule = workSchedulesRepository.findById(scheduleId)
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getUserId().equals(userId)) {
            throw CustomException.of(ScheduleErrorCode.UNAUTHORIZED_ACCESS);
        }

        return WorkScheduleResponse.from(schedule);
    }

    /**
     * 모든 접속자에게 스케줄 변경 알림을 전송합니다.
     */
    private void broadcastScheduleUpdate(List<ScheduleChange> changes) {
        if (changes.isEmpty()) {
            return;
        }

        List<ScheduleUpdateMessage.SlotUpdateInfo> updates = new ArrayList<>();

        for (ScheduleChange change : changes) {
            LocalDateTime current = change.getStart();
            LocalDateTime end = change.getEnd();

            while (current.isBefore(end)) {
                updates.add(ScheduleUpdateMessage.SlotUpdateInfo.builder()
                        .isAdd(change.isAdd())
                        .slotStartTime(current)
                        .build());
                current = current.plusMinutes(30);
            }
        }

        ScheduleUpdateMessage message = ScheduleUpdateMessage.builder()
                .type("SCHEDULE_UPDATED")
                .updates(updates)
                .build();

        messagingTemplate.convertAndSend("/topic/schedule-updates", message);
    }

    @Getter
    @AllArgsConstructor
    private static class ScheduleChange {
        private boolean isAdd;
        private LocalDateTime start;
        private LocalDateTime end;
    }
}