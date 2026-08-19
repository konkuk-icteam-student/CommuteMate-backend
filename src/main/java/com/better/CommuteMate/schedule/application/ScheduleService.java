package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.MonthlyWorkTimeExceededException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.WorkSlotUtils.SlotKey;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import com.better.CommuteMate.schedule.controller.dtos.ScheduleUpdateMessage;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkMonthlyScheduleResponse;
import com.better.CommuteMate.global.util.WorkWeekUtils;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditRequest;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleApplyPeriodResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleMonthlyLimitResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleSummaryResponse;
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
import java.util.LinkedHashMap;
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
    private final WorkChangeRequestRepository workChangeRequestRepository;
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
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);
    private static final int SLOT_MINUTES = 30;

    @Transactional
    public WorkScheduleChangeResultCommand changeWorkSchedules(WorkScheduleChangeCommand command) {
        validateChangeCommand(command);

        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        List<WorkScheduleSlotCommand> addSlots = command.addSlots();
        List<WorkScheduleSlotCommand> deleteSlots = command.deleteSlots();

        validateSlotsBasic(addSlots);
        validateSlotsBasic(deleteSlots);

        // 연월별 setting 로드: addSlots는 필수(없으면 404), deleteSlots는 Optional(없으면 단위 검증 스킵)
        Map<YearMonth, WorkScheduleSetting> addSettings =
                loadRequiredSettings(user.getOrganizationId(), addSlots);
        Map<YearMonth, WorkScheduleSetting> allSettings =
                loadOptionalSettings(user.getOrganizationId(), deleteSlots, addSettings);

        validateApplyPeriod(addSettings);
        validateSlotUnitAlignment(addSlots, allSettings);
        validateSlotUnitAlignment(deleteSlots, allSettings);

        validateMonthlyLimit(command.userId(), addSlots, deleteSlots);

        // 조회 일괄화: 사용자 슬롯 맵(중복·삭제 판단) + 날짜별 전체 근무(정원 판단)
        Set<LocalDate> allDates = new HashSet<>();
        addSlots.forEach(s -> allDates.add(s.date()));
        deleteSlots.forEach(s -> allDates.add(s.date()));

        Map<SlotKey, WorkSchedule> userScheduleMap = loadUserScheduleMap(command.userId(), allDates);

        Map<LocalDate, List<WorkSchedule>> daySchedules = new HashMap<>();
        addSlots.stream().map(WorkScheduleSlotCommand::date).distinct()
                .forEach(d -> daySchedules.put(d, workSchedulesRepository.findAllByDate(d)));

        Map<LocalDate, Set<SlotKey>> unavailableByDate = new HashMap<>();
        addSlots.stream().map(WorkScheduleSlotCommand::date).distinct()
                .forEach(d -> unavailableByDate.put(d, WorkSlotUtils.buildUnavailableSlotKeys(
                        workUnavailableTimeRepository.findBySettingAndDateBetween(
                                addSettings.get(YearMonth.from(d)), d, d),
                        WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES)));

        List<WorkScheduleChangeResponseDetail.Slot> success = new ArrayList<>();
        List<WorkScheduleChangeResponseDetail.Slot> failure = new ArrayList<>();
        List<ScheduleChange> changes = new ArrayList<>();
        List<WorkSchedule> toSave = new ArrayList<>();
        Set<SlotKey> tentativeSlotKeys = new HashSet<>();

        for (WorkScheduleSlotCommand slot : deleteSlots) {
            deleteSlotByRange(command.userId(), slot,
                    success, failure, changes, userScheduleMap);
        }

        for (WorkScheduleSlotCommand slot : addSlots) {
            WorkScheduleSetting setting = addSettings.get(YearMonth.from(slot.date()));
            processAddSlot(user, slot, setting,
                    success, failure, changes, toSave, daySchedules, userScheduleMap, tentativeSlotKeys,
                    unavailableByDate);
        }

        if (!toSave.isEmpty()) {
            workSchedulesRepository.saveAll(toSave);
        }

        broadcastScheduleUpdate(changes);
        return WorkScheduleChangeResultCommand.of(success, failure);
    }

    private void validateChangeCommand(WorkScheduleChangeCommand command) {
        if (command == null || command.isEmpty()) {
            throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
        }
    }

    private void validateApplyPeriod(Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        LocalDateTime now = LocalDateTime.now();
        for (WorkScheduleSetting setting : settingsByMonth.values()) {
            if (!setting.isApplyPeriod(now)) {
                throw CustomException.of(ScheduleErrorCode.APPLY_PERIOD_NOT_ACTIVE);
            }
        }
    }

    private void validateSlotsBasic(List<WorkScheduleSlotCommand> slots) {
        for (WorkScheduleSlotCommand slot : slots) {
            if (slot.date() == null || slot.start() == null || slot.end() == null) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }
            if (!slot.start().isBefore(slot.end())) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }
        }
    }

    private Map<YearMonth, WorkScheduleSetting> loadRequiredSettings(
            Long organizationId, List<WorkScheduleSlotCommand> slots) {
        Map<YearMonth, WorkScheduleSetting> map = new LinkedHashMap<>();
        for (WorkScheduleSlotCommand slot : slots) {
            YearMonth ym = YearMonth.from(slot.date());
            map.computeIfAbsent(ym, k -> workScheduleSettingService.getRequiredSetting(
                    organizationId, ym.getYear(), ym.getMonthValue()));
        }
        return map;
    }

    private Map<YearMonth, WorkScheduleSetting> loadOptionalSettings(
            Long organizationId, List<WorkScheduleSlotCommand> slots,
            Map<YearMonth, WorkScheduleSetting> alreadyLoaded) {
        Map<YearMonth, WorkScheduleSetting> map = new LinkedHashMap<>(alreadyLoaded);
        for (WorkScheduleSlotCommand slot : slots) {
            YearMonth ym = YearMonth.from(slot.date());
            if (!map.containsKey(ym)) {
                workScheduleSettingService.getSetting(organizationId, ym.getYear(), ym.getMonthValue())
                        .ifPresent(s -> map.put(ym, s));
            }
        }
        return map;
    }

    private void validateSlotUnitAlignment(
            List<WorkScheduleSlotCommand> slots,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        Map<LocalDate, List<WorkScheduleSlotCommand>> byDate = new LinkedHashMap<>();
        for (WorkScheduleSlotCommand slot : slots) {
            byDate.computeIfAbsent(slot.date(), k -> new ArrayList<>()).add(slot);
        }
        validateUnitAlignmentByDate(byDate, settingsByMonth);
    }

    /**
     * 날짜별로 그룹화된 슬롯에 대해 두 가지 검증을 수행한다.
     * (1) 각 슬롯의 start·end가 30분 경계에 맞는지 (SLOT_MINUTES 단위 정렬)
     * (2) 연속 구간을 병합한 뒤 각 구간 길이가 min_work_unit_minutes 이상인지 (하한 비교)
     * setting == null인 날짜는 검증을 스킵한다 (기존 동작 유지).
     */
    private void validateUnitAlignmentByDate(
            Map<LocalDate, List<WorkScheduleSlotCommand>> byDate,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        for (Map.Entry<LocalDate, List<WorkScheduleSlotCommand>> entry : byDate.entrySet()) {
            WorkScheduleSetting setting = settingsByMonth.get(YearMonth.from(entry.getKey()));
            if (setting == null) continue;
            List<WorkScheduleSlotCommand> dateSlots = entry.getValue();
            // (1) 30분 경계 정렬 검증
            for (WorkScheduleSlotCommand slot : dateSlots) {
                int startMin = slot.start().getHour() * 60 + slot.start().getMinute();
                int endMin = slot.end().getHour() * 60 + slot.end().getMinute();
                if (startMin % SLOT_MINUTES != 0 || endMin % SLOT_MINUTES != 0) {
                    throw CustomException.of(ScheduleErrorCode.INVALID_SLOT_BOUNDARY);
                }
            }
            // (2) 연속 구간 단위 최소 근무 시간 하한 검증
            int minMinutes = setting.getMinWorkUnitMinutes() != null
                    ? setting.getMinWorkUnitMinutes() : SLOT_MINUTES;
            for (WorkSlotUtils.TimeRange range : WorkSlotUtils.mergeConsecutiveRanges(dateSlots)) {
                if (Duration.between(range.start(), range.end()).toMinutes() < minMinutes) {
                    throw CustomException.of(ScheduleErrorCode.INVALID_SLOT_DURATION);
                }
            }
        }
    }

    private Map<SlotKey, WorkSchedule> loadUserScheduleMap(Long userId, Set<LocalDate> dates) {
        if (dates.isEmpty()) return new HashMap<>();
        LocalDate minDate = dates.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate maxDate = dates.stream().max(Comparator.naturalOrder()).orElseThrow();
        Map<SlotKey, WorkSchedule> map = new HashMap<>();
        workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId, minDate, maxDate, CodeType.WS04)
                .forEach(s -> map.put(new SlotKey(s.getDate(), s.getStartTime(), s.getEndTime()), s));
        return map;
    }

    /**
     * 원본 범위를 단위 슬롯으로 분할해 모두 찾아 취소한다.
     * 하나라도 존재하지 않거나 이미 취소 상태면 전체 범위를 실패로 처리한다.
     */
    private void deleteSlotByRange(
            Long userId,
            WorkScheduleSlotCommand originalSlot,
            List<WorkScheduleChangeResponseDetail.Slot> success,
            List<WorkScheduleChangeResponseDetail.Slot> failure,
            List<ScheduleChange> changes,
            Map<SlotKey, WorkSchedule> userScheduleMap) {

        List<WorkScheduleSlotCommand> unitSlots = WorkSlotUtils.splitIntoUnitSlots(
                originalSlot.date(), originalSlot.start(), originalSlot.end(), SLOT_MINUTES);

        List<WorkSchedule> toCancel = new ArrayList<>();
        for (WorkScheduleSlotCommand unitSlot : unitSlots) {
            SlotKey key = new SlotKey(unitSlot.date(), unitSlot.start(), unitSlot.end());
            WorkSchedule schedule = userScheduleMap.get(key);
            if (schedule == null || schedule.getStatusCode().equals(CodeType.WS04)) {
                failure.add(toResponseSlot(originalSlot));
                return;
            }
            toCancel.add(schedule);
        }

        for (WorkSchedule schedule : toCancel) {
            schedule.cancel(String.valueOf(userId));
            changes.add(new ScheduleChange(false,
                    LocalDateTime.of(schedule.getDate(), schedule.getStartTime()),
                    LocalDateTime.of(schedule.getDate(), schedule.getEndTime())));
        }
        success.add(toResponseSlot(originalSlot));
    }

    /**
     * 원본 범위를 단위 슬롯으로 분할해 중복·정원을 검증한 후 일괄 저장 목록에 추가한다.
     * 하나라도 실패하면 원본 범위 전체를 실패로 처리한다.
     */
    private void processAddSlot(
            User user,
            WorkScheduleSlotCommand originalSlot,
            WorkScheduleSetting setting,
            List<WorkScheduleChangeResponseDetail.Slot> success,
            List<WorkScheduleChangeResponseDetail.Slot> failure,
            List<ScheduleChange> changes,
            List<WorkSchedule> toSave,
            Map<LocalDate, List<WorkSchedule>> daySchedules,
            Map<SlotKey, WorkSchedule> userScheduleMap,
            Set<SlotKey> tentativeSlotKeys,
            Map<LocalDate, Set<SlotKey>> unavailableByDate) {

        List<WorkScheduleSlotCommand> unitSlots = WorkSlotUtils.splitIntoUnitSlots(
                originalSlot.date(), originalSlot.start(), originalSlot.end(), SLOT_MINUTES);

        // 모든 단위 슬롯을 먼저 검증
        for (WorkScheduleSlotCommand unitSlot : unitSlots) {
            SlotKey key = new SlotKey(unitSlot.date(), unitSlot.start(), unitSlot.end());

            if (unavailableByDate.getOrDefault(unitSlot.date(), Set.of()).contains(key)) {
                failure.add(toResponseSlot(originalSlot));
                return;
            }

            WorkSchedule existing = userScheduleMap.get(key);
            boolean isDuplicate = (existing != null && !existing.getStatusCode().equals(CodeType.WS04))
                    || tentativeSlotKeys.contains(key);
            if (isDuplicate) {
                failure.add(toResponseSlot(originalSlot));
                return;
            }

            List<WorkSchedule> dayList = daySchedules.getOrDefault(unitSlot.date(), List.of());
            if (!scheduleValidator.isScheduleInsertable(unitSlot, setting.getMaxConcurrentWorkers(), dayList)) {
                failure.add(toResponseSlot(originalSlot));
                return;
            }
        }

        // 모두 통과 → 저장 목록에 추가
        Workplace workplace = resolveWorkplace(user);
        for (WorkScheduleSlotCommand unitSlot : unitSlots) {
            SlotKey key = new SlotKey(unitSlot.date(), unitSlot.start(), unitSlot.end());
            tentativeSlotKeys.add(key);

            toSave.add(WorkSchedule.builder()
                    .user(user)
                    .setting(setting)
                    .workplace(workplace)
                    .date(unitSlot.date())
                    .startTime(unitSlot.start())
                    .endTime(unitSlot.end())
                    .statusCode(CodeType.WS02)
                    .createdBy(String.valueOf(user.getUserId()))
                    .updatedBy(String.valueOf(user.getUserId()))
                    .build());

            changes.add(new ScheduleChange(true,
                    LocalDateTime.of(unitSlot.date(), unitSlot.start()),
                    LocalDateTime.of(unitSlot.date(), unitSlot.end())));
        }
        success.add(toResponseSlot(originalSlot));
    }

    private void validateMonthlyLimit(
            Long userId,
            List<WorkScheduleSlotCommand> addSlots,
            List<WorkScheduleSlotCommand> deleteSlots) {
        if (addSlots.isEmpty()) return;

        YearMonth targetMonth = YearMonth.from(addSlots.get(0).date());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<WorkSchedule> currentSchedules =
                workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId, monthStart, monthEnd, CodeType.WS04);

        long currentMinutes = currentSchedules.stream()
                .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        long deleteMinutes = deleteSlots.stream()
                .filter(s -> YearMonth.from(s.date()).equals(targetMonth))
                .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes()).sum();

        long addMinutes = addSlots.stream()
                .filter(s -> YearMonth.from(s.date()).equals(targetMonth))
                .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes()).sum();

        long requestedMinutes = currentMinutes - deleteMinutes + addMinutes;

        if (requestedMinutes > MONTHLY_LIMIT_MINUTES) {
            throw new MonthlyWorkTimeExceededException(
                    MONTHLY_LIMIT_HOURS,
                    (int) Math.ceil(requestedMinutes / 60.0));
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
     * User 기준으로 근무지를 조회합니다.
     * User 엔티티 구조에 맞게 이 부분만 수정하면 됩니다.
     */
    private Workplace resolveWorkplace(User user) {
        return workplaceRepository.findFirstByOrganizationId(user.getOrganizationId())
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE));
    }

    private WorkScheduleChangeResponseDetail.Slot toResponseSlot(WorkScheduleSlotCommand slot) {
        return new WorkScheduleChangeResponseDetail.Slot(slot.startDateTime(), slot.endDateTime());
    }

    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> getWorkSchedules(Long userId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId, yearMonth.atDay(1), yearMonth.atEndOfMonth(), CodeType.WS04)
                .stream().map(WorkScheduleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkScheduleHistoryResponse> getWorkScheduleHistory(
            Long userId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
                        userId, yearMonth.atDay(1), yearMonth.atEndOfMonth(), CodeType.WS04);

        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();
        for (WorkSchedule schedule : schedules) {
            List<WorkAttendance> attendances =
                    workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

            Optional<WorkAttendance> checkIn = attendances.stream()
                    .filter(a -> a.getCheckTypeCode() == CodeType.CT01).findFirst();
            Optional<WorkAttendance> checkOut = attendances.stream()
                    .filter(a -> a.getCheckTypeCode() == CodeType.CT02).findFirst();

            LocalDateTime actualStart = checkIn.map(WorkAttendance::getCheckTime).orElse(null);
            LocalDateTime actualEnd = checkOut.map(WorkAttendance::getCheckTime).orElse(null);
            Long duration = (actualStart != null && actualEnd != null)
                    ? Duration.between(actualStart, actualEnd).toMinutes() : null;

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

    @Transactional(readOnly = true)
    public WorkScheduleResponse getWorkSchedule(Long userId, Long scheduleId) {
        WorkSchedule schedule = workSchedulesRepository.findById(scheduleId)
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw CustomException.of(ScheduleErrorCode.UNAUTHORIZED_ACCESS);
        }
        return WorkScheduleResponse.from(schedule);
    }

    private void broadcastScheduleUpdate(List<ScheduleChange> changes) {
        if (changes.isEmpty()) return;

        List<ScheduleUpdateMessage.SlotUpdateInfo> updates = new ArrayList<>();
        for (ScheduleChange change : changes) {
            LocalDateTime current = change.getStart();
            while (current.isBefore(change.getEnd())) {
                updates.add(ScheduleUpdateMessage.SlotUpdateInfo.builder()
                        .isAdd(change.isAdd())
                        .slotStartTime(current)
                        .build());
                current = current.plusMinutes(SLOT_MINUTES);
            }
        }

        messagingTemplate.convertAndSend("/topic/schedule-updates",
                ScheduleUpdateMessage.builder().type("SCHEDULE_UPDATED").updates(updates).build());
    }

    @Transactional(readOnly = true)
    public WorkScheduleMonthlyLimitResponse getMonthlyLimit(
            Long organizationId, Integer year, Integer month) {
        validateYearMonth(year, month);
        WorkScheduleSetting setting = workScheduleSettingService.getRequiredSetting(organizationId, year, month);
        Integer maxConcurrent = setting.getMaxConcurrentWorkers() != null
                ? setting.getMaxConcurrentWorkers() : DEFAULT_SETTING_MAX_CONCURRENT;
        return WorkScheduleMonthlyLimitResponse.builder()
                .scheduleYear(year).scheduleMonth(month).maxConcurrentWorkers(maxConcurrent).build();
    }

    /**
     * 근로 신청 기간 조회 (프론트 "근로 신청"/"수정 요청" 버튼 활성화 판단용).
     * isApplyAvailable은 setting.isApplyPeriod(now)를 그대로 사용한다
     * (isApplyPeriod는 날짜 단위로 종료일 당일까지 포함해 판정한다).
     */
    @Transactional(readOnly = true)
    public WorkScheduleApplyPeriodResponse getApplyPeriod(Long organizationId, Integer year, Integer month) {
        validateYearMonth(year, month);

        Optional<WorkScheduleSetting> settingOpt =
                workScheduleSettingService.getSetting(organizationId, year, month);

        if (settingOpt.isEmpty()) {
            return WorkScheduleApplyPeriodResponse.builder()
                    .applyStartDate(null)
                    .applyEndDate(null)
                    .isApplyAvailable(false)
                    .isEditAvailable(true)
                    .build();
        }

        WorkScheduleSetting setting = settingOpt.get();
        boolean isApplyAvailable = setting.isApplyPeriod(LocalDateTime.now());
        boolean isEditAvailable = !isApplyAvailable;

        return WorkScheduleApplyPeriodResponse.builder()
                .applyStartDate(setting.getApplyStartAt().toLocalDate())
                .applyEndDate(setting.getApplyEndAt().toLocalDate())
                .isApplyAvailable(isApplyAvailable)
                .isEditAvailable(isEditAvailable)
                .build();
    }

    @Transactional
    public WorkScheduleEditResponse submitEditRequest(Long userId, WorkScheduleEditRequest request) {
        List<WorkScheduleEditRequest.Slot> deleteSlots = request.deleteSlotsOrEmpty();
        List<WorkScheduleEditRequest.Slot> addSlots = request.addSlotsOrEmpty();

        if (deleteSlots.isEmpty() && addSlots.isEmpty()) {
            throw CustomException.of(ScheduleErrorCode.EDIT_REQUEST_EMPTY);
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw CustomException.of(ScheduleErrorCode.EDIT_REQUEST_REASON_REQUIRED);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 단위 정렬 검증을 위해 관련 setting 로드 (Optional)
        Map<YearMonth, WorkScheduleSetting> settingsForEdit = new HashMap<>();
        addSlots.stream().map(s -> YearMonth.from(s.date())).distinct().forEach(ym ->
                workScheduleSettingService.getSetting(user.getOrganizationId(), ym.getYear(), ym.getMonthValue())
                        .ifPresent(s -> settingsForEdit.put(ym, s)));
        deleteSlots.stream().map(s -> YearMonth.from(s.date())).distinct().forEach(ym -> {
            if (!settingsForEdit.containsKey(ym)) {
                workScheduleSettingService.getSetting(user.getOrganizationId(), ym.getYear(), ym.getMonthValue())
                        .ifPresent(s -> settingsForEdit.put(ym, s));
            }
        });

        validateEditSlotUnitAlignment(addSlots, settingsForEdit);
        validateEditSlotUnitAlignment(deleteSlots, settingsForEdit);
        validateCombinedDurationForEdit(userId, addSlots, deleteSlots, settingsForEdit);

        validateMonthlyLimitForEdit(userId, user.getOrganizationId(), addSlots, deleteSlots);
        validateUnavailableForEditAdd(addSlots, settingsForEdit);
        validateNotInApplyPeriodForEdit(addSlots, deleteSlots, settingsForEdit);

        WorkChangeRequest changeRequest = WorkChangeRequest.builder()
                .user(user).reason(request.reason()).statusCode(CodeType.CS01)
                .createdBy(userId).updatedBy(userId).build();
        workChangeRequestRepository.save(changeRequest);

        for (WorkScheduleEditRequest.Slot slot : deleteSlots) {
            for (WorkScheduleSlotCommand unitSlot : WorkSlotUtils.splitIntoUnitSlots(
                    slot.date(), slot.start(), slot.end(), SLOT_MINUTES)) {
                workSchedulesRepository
                        .findByUser_UserIdAndDateAndStartTimeAndEndTime(
                                userId, unitSlot.date(), unitSlot.start(), unitSlot.end())
                        .filter(s -> !s.getStatusCode().equals(CodeType.WS04))
                        .orElseThrow(() -> CustomException.of(ScheduleErrorCode.DELETE_SCHEDULE_NOT_FOUND));
            }
            // 이력 보존을 위해 원본 범위를 그대로 저장한다.
            // schedule FK는 범위 분할로 다수 슬롯이 되므로 null 처리.
            // 어드민 승인 처리(AdminWorkChangeRequestProcessService)도 함께 수정 필요 — 리포트 참고.
            workChangeRequestItemRepository.save(WorkChangeRequestItem.builder()
                    .request(changeRequest).changeTypeCode(CodeType.CR02)
                    .schedule(null)
                    .date(slot.date()).startTime(slot.start()).endTime(slot.end()).build());
        }

        for (WorkScheduleEditRequest.Slot slot : addSlots) {
            workChangeRequestItemRepository.save(WorkChangeRequestItem.builder()
                    .request(changeRequest).changeTypeCode(CodeType.CR01)
                    .schedule(null)
                    .date(slot.date()).startTime(slot.start()).endTime(slot.end()).build());
        }

        return WorkScheduleEditResponse.builder()
                .requestId(changeRequest.getRequestId())
                .status(CodeType.CS01.getCodeName()).build();
    }

    private void validateUnavailableForEditAdd(
            List<WorkScheduleEditRequest.Slot> addSlots,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        Map<LocalDate, Set<SlotKey>> unavailableByDate = new HashMap<>();
        for (WorkScheduleEditRequest.Slot slot : addSlots) {
            Set<SlotKey> unavailable = unavailableByDate.computeIfAbsent(slot.date(), d -> {
                WorkScheduleSetting s = settingsByMonth.get(YearMonth.from(d));
                if (s == null) return Set.of();
                return WorkSlotUtils.buildUnavailableSlotKeys(
                        workUnavailableTimeRepository.findBySettingAndDateBetween(s, d, d),
                        WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES);
            });
            for (WorkScheduleSlotCommand unit : WorkSlotUtils.splitIntoUnitSlots(
                    slot.date(), slot.start(), slot.end(), SLOT_MINUTES)) {
                if (unavailable.contains(new SlotKey(unit.date(), unit.start(), unit.end()))) {
                    throw CustomException.of(ScheduleErrorCode.UNAVAILABLE_TIME_CONFLICT);
                }
            }
        }
    }

    /**
     * 대상 슬롯 date가 속한 월이 "지금" 신청 기간이면 그 월에 대한 수정 요청을 차단한다.
     * 신청 기간 여부는 요청 시점(now)이 아니라 각 슬롯의 월 setting 기준으로 판정하며,
     * 여러 월이 섞인 요청은 하나라도 걸리면 요청 전체를 거부한다(다른 edit 검증들과 동일하게
     * 전체 실패 방식 — 부분 수용 시 일부만 WorkChangeRequestItem이 생성되는 혼란을 피한다).
     * setting이 없는 월은 신청 기간이 아닌 것으로 간주해 허용한다(getApplyPeriod의
     * isEditAvailable 기본값과 동일한 판단 기준).
     */
    private void validateNotInApplyPeriodForEdit(
            List<WorkScheduleEditRequest.Slot> addSlots,
            List<WorkScheduleEditRequest.Slot> deleteSlots,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        LocalDateTime now = LocalDateTime.now();
        Set<YearMonth> targetMonths = new HashSet<>();
        addSlots.forEach(slot -> targetMonths.add(YearMonth.from(slot.date())));
        deleteSlots.forEach(slot -> targetMonths.add(YearMonth.from(slot.date())));

        for (YearMonth ym : targetMonths) {
            WorkScheduleSetting setting = settingsByMonth.get(ym);
            if (setting != null && setting.isApplyPeriod(now)) {
                throw CustomException.of(ScheduleErrorCode.EDIT_NOT_ALLOWED_DURING_APPLY_PERIOD);
            }
        }
    }

    private void validateEditSlotUnitAlignment(
            List<WorkScheduleEditRequest.Slot> slots,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        for (WorkScheduleEditRequest.Slot slot : slots) {
            WorkScheduleSetting setting = settingsByMonth.get(YearMonth.from(slot.date()));
            if (setting == null) continue;
            int startMin = slot.start().getHour() * 60 + slot.start().getMinute();
            int endMin = slot.end().getHour() * 60 + slot.end().getMinute();
            if (startMin % SLOT_MINUTES != 0 || endMin % SLOT_MINUTES != 0) {
                throw CustomException.of(ScheduleErrorCode.INVALID_SLOT_BOUNDARY);
            }
        }
    }

    /**
     * 날짜별 슬롯 집합을 병합한 연속 구간이 min_work_unit_minutes 이상인지 검증한다.
     * 호출자(apply: 요청 슬롯만, edit: DB−삭제+추가 합산)가 byDate를 구성해 전달한다.
     * setting == null인 날짜는 검증을 스킵한다.
     */
    private void validateMinDurationByDate(
            Map<LocalDate, List<WorkScheduleSlotCommand>> byDate,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        for (Map.Entry<LocalDate, List<WorkScheduleSlotCommand>> entry : byDate.entrySet()) {
            WorkScheduleSetting setting = settingsByMonth.get(YearMonth.from(entry.getKey()));
            if (setting == null) continue;
            int minMinutes = setting.getMinWorkUnitMinutes() != null
                    ? setting.getMinWorkUnitMinutes() : SLOT_MINUTES;
            for (WorkSlotUtils.TimeRange range : WorkSlotUtils.mergeConsecutiveRanges(entry.getValue())) {
                if (Duration.between(range.start(), range.end()).toMinutes() < minMinutes) {
                    throw CustomException.of(ScheduleErrorCode.INVALID_SLOT_DURATION);
                }
            }
        }
    }

    /**
     * edit 전용: (DB ACTIVE 슬롯) − (deleteSlots) + (addSlots) 합산 집합으로 날짜별 하한 검증.
     * addSlots와 deleteSlots는 30분 단위로 분할해 DB 저장 단위와 동일한 입도로 처리한다.
     */
    private void validateCombinedDurationForEdit(
            Long userId,
            List<WorkScheduleEditRequest.Slot> addSlots,
            List<WorkScheduleEditRequest.Slot> deleteSlots,
            Map<YearMonth, WorkScheduleSetting> settingsByMonth) {
        Set<LocalDate> allDates = new HashSet<>();
        addSlots.forEach(s -> allDates.add(s.date()));
        deleteSlots.forEach(s -> allDates.add(s.date()));
        if (allDates.isEmpty()) return;

        Set<WorkScheduleSlotCommand> deleteSet = new HashSet<>();
        for (WorkScheduleEditRequest.Slot slot : deleteSlots) {
            deleteSet.addAll(WorkSlotUtils.splitIntoUnitSlots(
                    slot.date(), slot.start(), slot.end(), SLOT_MINUTES));
        }

        LocalDate minDate = allDates.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate maxDate = allDates.stream().max(Comparator.naturalOrder()).orElseThrow();

        Map<LocalDate, List<WorkScheduleSlotCommand>> byDate = new LinkedHashMap<>();
        for (WorkSchedule s : workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                userId, minDate, maxDate, ACTIVE_STATUSES)) {
            WorkScheduleSlotCommand cmd = new WorkScheduleSlotCommand(
                    s.getDate(), s.getStartTime(), s.getEndTime());
            if (!deleteSet.contains(cmd)) {
                byDate.computeIfAbsent(s.getDate(), k -> new ArrayList<>()).add(cmd);
            }
        }

        for (WorkScheduleEditRequest.Slot slot : addSlots) {
            for (WorkScheduleSlotCommand unit : WorkSlotUtils.splitIntoUnitSlots(
                    slot.date(), slot.start(), slot.end(), SLOT_MINUTES)) {
                byDate.computeIfAbsent(slot.date(), k -> new ArrayList<>()).add(unit);
            }
        }

        byDate.keySet().retainAll(allDates);

        validateMinDurationByDate(byDate, settingsByMonth);
    }

    private void validateMonthlyLimitForEdit(
            Long userId, Long organizationId,
            List<WorkScheduleEditRequest.Slot> addSlots,
            List<WorkScheduleEditRequest.Slot> deleteSlots) {
        if (addSlots.isEmpty()) return;

        YearMonth targetMonth = YearMonth.from(addSlots.get(0).date());
        Optional<WorkScheduleSetting> settingOpt =
                workScheduleSettingService.getSetting(organizationId, targetMonth.getYear(), targetMonth.getMonthValue());
        if (settingOpt.isEmpty()) return;

        WorkScheduleSetting setting = settingOpt.get();
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        long currentMinutes = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(userId, monthStart, monthEnd, CodeType.WS04)
                .stream().mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        long deleteMinutes = deleteSlots.stream()
                .filter(s -> YearMonth.from(s.date()).equals(targetMonth))
                .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes()).sum();

        long addMinutes = addSlots.stream()
                .filter(s -> YearMonth.from(s.date()).equals(targetMonth))
                .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes()).sum();

        long requestedMinutes = currentMinutes - deleteMinutes + addMinutes;
        int limitMinutes = setting.getMonthlyRequiredMinutes();

        if (requestedMinutes > limitMinutes) {
            throw new MonthlyWorkTimeExceededException(
                    limitMinutes / 60, (int) Math.ceil(requestedMinutes / 60.0));
        }
    }

    @Transactional(readOnly = true)
    public WorkMonthlyScheduleResponse getMonthlyScheduleView(
            Long userId, Long organizationId, Integer year, Integer month) {
        validateYearMonth(year, month);
        WorkScheduleSetting setting = workScheduleSettingService.getRequiredSetting(organizationId, year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        SlotViewContext ctx = buildSlotViewContext(userId, setting, monthStart, monthEnd, monthStart, monthEnd);
        List<WorkMonthlyScheduleResponse.DaySchedule> days = buildDaySlotList(monthStart, monthEnd, ctx);

        return WorkMonthlyScheduleResponse.builder()
                .year(year).month(month)
                .maxConcurrentWorkers(setting.getMaxConcurrentWorkers())
                .totalLimitHours(setting.getMonthlyRequiredMinutes() / 60)
                .usedHours(ctx.usedHours()).days(days).build();
    }

    @Transactional(readOnly = true)
    public WorkScheduleSummaryResponse getScheduleSummary(
            Long userId, Long organizationId, LocalDate startDate, LocalDate endDate) {
        validateSummaryDateRange(startDate, endDate);

        Optional<WorkScheduleSetting> settingOpt =
                workScheduleSettingService.getSetting(organizationId, startDate.getYear(), startDate.getMonthValue());

        long weekUsedMinutes = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(userId, startDate, endDate, ACTIVE_STATUSES)
                .stream().mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        YearMonth yearMonth = YearMonth.from(startDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        long monthUsedMinutes = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(userId, monthStart, monthEnd, ACTIVE_STATUSES)
                .stream().mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        int minWorkUnitMinutes = settingOpt
                .map(s -> s.getMinWorkUnitMinutes() != null ? s.getMinWorkUnitMinutes() : 30).orElse(30);
        int weekMinHours = settingOpt
                .map(s -> s.getWeeklyMinMinutes() != null ? s.getWeeklyMinMinutes() / 60 : 0).orElse(0);
        int weekMaxHours = settingOpt
                .map(s -> s.getWeeklyMaxMinutes() != null ? s.getWeeklyMaxMinutes() / 60 : 0).orElse(0);
        int monthMinHours = settingOpt
                .map(s -> s.getMonthlyMinMinutes() != null ? s.getMonthlyMinMinutes() / 60 : 0).orElse(0);
        int monthMaxHours = settingOpt.map(s -> {
            if (s.getMonthlyMaxMinutes() != null) return s.getMonthlyMaxMinutes() / 60;
            if (s.getMonthlyRequiredMinutes() != null) return s.getMonthlyRequiredMinutes() / 60;
            return 0;
        }).orElse(0);
        int weekNumber = WorkWeekUtils.weekOfMonth(startDate);

        return WorkScheduleSummaryResponse.builder()
                .startDate(startDate).endDate(endDate)
                .minWorkUnitMinutes(minWorkUnitMinutes)
                .week(WorkScheduleSummaryResponse.PeriodSummary.builder()
                        .label(weekNumber + "주차")
                        .usedHours((int) (weekUsedMinutes / 60))
                        .minHours(weekMinHours).maxHours(weekMaxHours).build())
                .month(WorkScheduleSummaryResponse.PeriodSummary.builder()
                        .label(startDate.getMonthValue() + "월 전체")
                        .usedHours((int) (monthUsedMinutes / 60))
                        .minHours(monthMinHours).maxHours(monthMaxHours).build())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkScheduleRangeResponse getScheduleRangeView(
            Long userId, Long organizationId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        WorkScheduleSetting setting = workScheduleSettingService
                .getSetting(organizationId, startDate.getYear(), startDate.getMonthValue()).orElse(null);

        YearMonth yearMonth = YearMonth.from(startDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        SlotViewContext ctx = buildSlotViewContext(userId, setting, startDate, endDate, monthStart, monthEnd);
        List<WorkMonthlyScheduleResponse.DaySchedule> days = buildDaySlotList(startDate, endDate, ctx);

        Integer maxConcurrent = setting != null && setting.getMaxConcurrentWorkers() != null
                ? setting.getMaxConcurrentWorkers()
                : DEFAULT_SETTING_MAX_CONCURRENT;
        Integer totalLimitHours = setting != null ? setting.getMonthlyRequiredMinutes() / 60 : null;

        return WorkScheduleRangeResponse.builder()
                .startDate(startDate).endDate(endDate)
                .maxConcurrentWorkers(maxConcurrent).totalLimitHours(totalLimitHours)
                .usedHours(ctx.usedHours()).days(days).build();
    }

    private Set<SlotKey> buildItemSlots(
            Long userId, CodeType changeTypeCode, LocalDate startDate, LocalDate endDate) {
        Set<SlotKey> slots = new HashSet<>();
        workChangeRequestItemRepository
                .findByRequest_User_UserIdAndRequest_StatusCodeAndChangeTypeCodeAndDateBetween(
                        userId, CodeType.CS01, changeTypeCode, startDate, endDate)
                .forEach(item -> slots.addAll(WorkSlotUtils.expandToSlots(item.getDate(), item.getStartTime(), item.getEndTime(), SLOT_MINUTES)));
        return slots;
    }

    private void validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null || year < 1900 || year > 9999 || month < 1 || month > 12) {
            throw CustomException.of(ScheduleErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw CustomException.of(ScheduleErrorCode.INVALID_DATE_RANGE);
        if (!YearMonth.from(startDate).equals(YearMonth.from(endDate)))
            throw CustomException.of(ScheduleErrorCode.CROSS_MONTH_RANGE_NOT_ALLOWED);
    }

    private void validateSummaryDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw CustomException.of(ScheduleErrorCode.INVALID_DATE_RANGE);
        if (!YearMonth.from(startDate).equals(YearMonth.from(endDate)))
            throw CustomException.of(ScheduleErrorCode.CROSS_MONTH_RANGE_NOT_ALLOWED);
        if (!WorkWeekUtils.isSameWeek(startDate, endDate))
            throw CustomException.of(ScheduleErrorCode.CROSS_WEEK_RANGE_NOT_ALLOWED);
    }

    private SlotViewContext buildSlotViewContext(
            Long userId, WorkScheduleSetting setting,
            LocalDate queryStart, LocalDate queryEnd,
            LocalDate monthStart, LocalDate monthEnd) {
        Map<SlotKey, Integer> currentCountMap = new HashMap<>();
        for (WorkSchedule s : workSchedulesRepository.findAllByDateBetweenAndStatusCodeIn(
                queryStart, queryEnd, ACTIVE_STATUSES)) {
            for (SlotKey k : WorkSlotUtils.expandToSlots(s.getDate(), s.getStartTime(), s.getEndTime(), SLOT_MINUTES))
                currentCountMap.merge(k, 1, Integer::sum);
        }

        Set<SlotKey> myScheduleSlots = new HashSet<>();
        for (WorkSchedule s : workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                userId, queryStart, queryEnd, ACTIVE_STATUSES))
            myScheduleSlots.addAll(WorkSlotUtils.expandToSlots(s.getDate(), s.getStartTime(), s.getEndTime(), SLOT_MINUTES));

        Set<SlotKey> pendingDeleteSlots = buildItemSlots(userId, CodeType.CR02, queryStart, queryEnd);
        Set<SlotKey> pendingAddSlots = buildItemSlots(userId, CodeType.CR01, queryStart, queryEnd);

        Set<SlotKey> unavailableSlots = setting == null ? new HashSet<>()
                : WorkSlotUtils.buildUnavailableSlotKeys(
                        workUnavailableTimeRepository.findBySettingAndDateBetween(setting, queryStart, queryEnd),
                        WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES);

        long usedMinutes = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(userId, monthStart, monthEnd, ACTIVE_STATUSES)
                .stream().mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();

        return new SlotViewContext(currentCountMap, myScheduleSlots, pendingDeleteSlots,
                pendingAddSlots, unavailableSlots, (int) (usedMinutes / 60));
    }

    private List<WorkMonthlyScheduleResponse.DaySchedule> buildDaySlotList(
            LocalDate startDate, LocalDate endDate, SlotViewContext ctx) {
        List<WorkMonthlyScheduleResponse.DaySchedule> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<WorkMonthlyScheduleResponse.SlotInfo> slots = new ArrayList<>();
            LocalTime current = WORK_START_TIME;
            while (current.isBefore(WORK_END_TIME)) {
                LocalTime next = current.plusMinutes(SLOT_MINUTES);
                SlotKey key = new SlotKey(date, current, next);
                slots.add(WorkMonthlyScheduleResponse.SlotInfo.builder()
                        .start(current).end(next)
                        .status(resolveSlotStatus(key, ctx.myScheduleSlots(), ctx.pendingDeleteSlots(),
                                ctx.pendingAddSlots(), ctx.unavailableSlots()))
                        .currentCount(ctx.currentCountMap().getOrDefault(key, 0)).build());
                current = next;
            }
            days.add(WorkMonthlyScheduleResponse.DaySchedule.builder().date(date).slots(slots).build());
        }
        return days;
    }

    private String resolveSlotStatus(SlotKey key, Set<SlotKey> myScheduleSlots,
            Set<SlotKey> pendingDeleteSlots, Set<SlotKey> pendingAddSlots, Set<SlotKey> unavailableSlots) {
        if (myScheduleSlots.contains(key)) return "MY_SCHEDULE";
        if (pendingDeleteSlots.contains(key)) return "PENDING_DELETE";
        if (pendingAddSlots.contains(key)) return "PENDING_ADD";
        if (unavailableSlots.contains(key)) return "UNAVAILABLE";
        return "EMPTY";
    }

    private record SlotViewContext(
            Map<SlotKey, Integer> currentCountMap,
            Set<SlotKey> myScheduleSlots,
            Set<SlotKey> pendingDeleteSlots,
            Set<SlotKey> pendingAddSlots,
            Set<SlotKey> unavailableSlots,
            int usedHours) {}

    @Getter
    @AllArgsConstructor
    private static class ScheduleChange {
        private boolean isAdd;
        private LocalDateTime start;
        private LocalDateTime end;
    }
}
