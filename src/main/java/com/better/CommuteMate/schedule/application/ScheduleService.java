package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
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
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkMonthlyScheduleResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleMonthlyLimitResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
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
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final WorkChangeRequestItemRepository workChangeRequestItemRepository;
    private final WorkUnavailableTimeRepository workUnavailableTimeRepository;
    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ScheduleValidator scheduleValidator;
    private final WorkScheduleSettingService workScheduleSettingService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int MONTHLY_LIMIT_HOURS = 27;
    private static final int MONTHLY_LIMIT_MINUTES = MONTHLY_LIMIT_HOURS * 60;
    private static final List<CodeType> ACTIVE_STATUSES = List.of(CodeType.WS01, CodeType.WS02);
    private static final int DEFAULT_SETTING_MAX_CONCURRENT = 4;

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

    @Transactional(readOnly = true)
    public WorkScheduleMonthlyLimitResponse getMonthlyLimit(
            String organizationId,
            Integer year,
            Integer month
    ) {
        validateYearMonth(year, month);
        WorkScheduleSetting setting = workScheduleSettingService.getRequiredSetting(organizationId, year, month);
        Integer maxConcurrent = setting.getMaxConcurrentWorkers() != null
                ? setting.getMaxConcurrentWorkers()
                : DEFAULT_SETTING_MAX_CONCURRENT;
        return WorkScheduleMonthlyLimitResponse.builder()
                .scheduleYear(year)
                .scheduleMonth(month)
                .maxConcurrentWorkers(maxConcurrent)
                .build();
    }

    @Transactional(readOnly = true)
    public WorkMonthlyScheduleResponse getMonthlyScheduleView(
            Long userId,
            String organizationId,
            Integer year,
            Integer month
    ) {
        validateYearMonth(year, month);
        WorkScheduleSetting setting = workScheduleSettingService.getRequiredSetting(organizationId, year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        SlotViewContext ctx = buildSlotViewContext(userId, setting, monthStart, monthEnd, monthStart, monthEnd);
        Set<SlotKey> visibleSlots = buildVisibleSlots(ctx);
        List<WorkMonthlyScheduleResponse.DaySchedule> days = buildDaySlotList(monthStart, monthEnd, visibleSlots, ctx);

        return WorkMonthlyScheduleResponse.builder()
                .year(year)
                .month(month)
                .maxConcurrentWorkers(setting.getMaxConcurrentWorkers())
                .totalLimitHours(setting.getMonthlyRequiredMinutes() / 60)
                .usedHours(ctx.usedHours())
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public WorkScheduleRangeResponse getScheduleRangeView(
            Long userId,
            String organizationId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        WorkScheduleSetting setting = workScheduleSettingService
                .getSetting(organizationId, startDate.getYear(), startDate.getMonthValue())
                .orElse(null);

        YearMonth yearMonth = YearMonth.from(startDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        SlotViewContext ctx = buildSlotViewContext(userId, setting, startDate, endDate, monthStart, monthEnd);
        Set<SlotKey> visibleSlots = buildVisibleSlots(ctx);
        List<WorkMonthlyScheduleResponse.DaySchedule> days = buildDaySlotList(startDate, endDate, visibleSlots, ctx);

        Integer maxConcurrent = setting != null ? setting.getMaxConcurrentWorkers() : DEFAULT_SETTING_MAX_CONCURRENT;
        Integer totalLimitHours = setting != null ? setting.getMonthlyRequiredMinutes() / 60 : null;

        return WorkScheduleRangeResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .maxConcurrentWorkers(maxConcurrent)
                .totalLimitHours(totalLimitHours)
                .usedHours(ctx.usedHours())
                .days(days)
                .build();
    }

    private Set<SlotKey> buildItemSlots(
            Long userId, CodeType changeTypeCode, LocalDate startDate, LocalDate endDate) {
        Set<SlotKey> slots = new HashSet<>();
        List<WorkChangeRequestItem> items =
                workChangeRequestItemRepository
                        .findByRequest_User_UserIdAndRequest_StatusCodeAndChangeTypeCodeAndDateBetween(
                                userId, CodeType.CS01, changeTypeCode, startDate, endDate);
        for (WorkChangeRequestItem item : items) {
            slots.addAll(expandToSlots(item.getDate(), item.getStartTime(), item.getEndTime()));
        }
        return slots;
    }

    private void validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null || year < 1900 || year > 9999 || month < 1 || month > 12) {
            throw CustomException.of(ScheduleErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw CustomException.of(ScheduleErrorCode.INVALID_DATE_RANGE);
        }
        if (!YearMonth.from(startDate).equals(YearMonth.from(endDate))) {
            throw CustomException.of(ScheduleErrorCode.CROSS_MONTH_RANGE_NOT_ALLOWED);
        }
    }

    private SlotViewContext buildSlotViewContext(
            Long userId,
            WorkScheduleSetting setting,
            LocalDate queryStart,
            LocalDate queryEnd,
            LocalDate monthStart,
            LocalDate monthEnd
    ) {
        Map<SlotKey, Integer> currentCountMap = new HashMap<>();
        for (WorkSchedule s : workSchedulesRepository.findAllByDateBetweenAndStatusCodeIn(queryStart, queryEnd, ACTIVE_STATUSES)) {
            for (SlotKey k : expandToSlots(s.getDate(), s.getStartTime(), s.getEndTime()))
                currentCountMap.merge(k, 1, Integer::sum);
        }

        Set<SlotKey> myScheduleSlots = new HashSet<>();
        for (WorkSchedule s : workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                userId, queryStart, queryEnd, ACTIVE_STATUSES))
            myScheduleSlots.addAll(expandToSlots(s.getDate(), s.getStartTime(), s.getEndTime()));

        Set<SlotKey> pendingDeleteSlots = buildItemSlots(userId, CodeType.CR02, queryStart, queryEnd);
        Set<SlotKey> pendingAddSlots = buildItemSlots(userId, CodeType.CR01, queryStart, queryEnd);

        Set<SlotKey> unavailableSlots = new HashSet<>();
        if (setting != null) {
            for (WorkUnavailableTime u : workUnavailableTimeRepository.findBySettingAndDateBetween(setting, queryStart, queryEnd))
                unavailableSlots.addAll(expandToSlots(u.getDate(), u.getStartTime(), u.getEndTime()));
        }

        long usedMinutes = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(userId, monthStart, monthEnd, ACTIVE_STATUSES)
                .stream().mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        return new SlotViewContext(currentCountMap, myScheduleSlots, pendingDeleteSlots, pendingAddSlots,
                unavailableSlots, (int) (usedMinutes / 60));
    }

    private Set<SlotKey> buildVisibleSlots(SlotViewContext ctx) {
        Set<SlotKey> visibleSlots = new HashSet<>(ctx.currentCountMap().keySet());
        visibleSlots.addAll(ctx.unavailableSlots());
        visibleSlots.addAll(ctx.pendingAddSlots());
        return visibleSlots;
    }

    private List<WorkMonthlyScheduleResponse.DaySchedule> buildDaySlotList(
            LocalDate startDate,
            LocalDate endDate,
            Set<SlotKey> visibleSlots,
            SlotViewContext ctx
    ) {
        List<WorkMonthlyScheduleResponse.DaySchedule> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate d = date;
            List<WorkMonthlyScheduleResponse.SlotInfo> slots = visibleSlots.stream()
                    .filter(k -> k.date().equals(d))
                    .sorted(Comparator.comparing(SlotKey::startTime))
                    .map(k -> WorkMonthlyScheduleResponse.SlotInfo.builder()
                            .start(k.startTime())
                            .end(k.endTime())
                            .status(resolveSlotStatus(k, ctx.myScheduleSlots(), ctx.pendingDeleteSlots(),
                                    ctx.pendingAddSlots(), ctx.unavailableSlots()))
                            .currentCount(ctx.currentCountMap().getOrDefault(k, 0))
                            .build())
                    .toList();
            days.add(WorkMonthlyScheduleResponse.DaySchedule.builder().date(d).slots(slots).build());
        }
        return days;
    }

    private String resolveSlotStatus(
            SlotKey key,
            Set<SlotKey> myScheduleSlots,
            Set<SlotKey> pendingDeleteSlots,
            Set<SlotKey> pendingAddSlots,
            Set<SlotKey> unavailableSlots
    ) {
        if (myScheduleSlots.contains(key)) return "MY_SCHEDULE";
        if (pendingDeleteSlots.contains(key)) return "PENDING_DELETE";
        if (pendingAddSlots.contains(key)) return "PENDING_ADD";
        if (unavailableSlots.contains(key)) return "UNAVAILABLE";
        return "EMPTY";
    }

    private List<SlotKey> expandToSlots(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<SlotKey> slots = new ArrayList<>();
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            LocalTime next = current.plusMinutes(30);
            slots.add(new SlotKey(date, current, next));
            current = next;
        }
        return slots;
    }

    private record SlotViewContext(
            Map<SlotKey, Integer> currentCountMap,
            Set<SlotKey> myScheduleSlots,
            Set<SlotKey> pendingDeleteSlots,
            Set<SlotKey> pendingAddSlots,
            Set<SlotKey> unavailableSlots,
            int usedHours
    ) {}

    private record SlotKey(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    @Getter
    @AllArgsConstructor
    private static class ScheduleChange {
        private boolean isAdd;
        private LocalDateTime start;
        private LocalDateTime end;
    }
}