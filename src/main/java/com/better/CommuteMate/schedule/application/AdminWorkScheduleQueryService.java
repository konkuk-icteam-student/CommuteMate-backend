package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.schedule.application.WorkSlotUtils.SlotKey;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminScheduleRangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWorkScheduleQueryService {

    private static final List<CodeType> ACTIVE_STATUSES = List.of(CodeType.WS01, CodeType.WS02);
    private static final int SLOT_MINUTES = 30;
    private static final int DEFAULT_MAX_CONCURRENT_WORKERS = 4;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME   = LocalTime.of(18, 0);

    private final WorkScheduleSettingRepository settingRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkUnavailableTimeRepository unavailableTimeRepository;
    private final WorkAttendanceRepository attendanceRepository;

    public AdminScheduleRangeResponse getSchedules(
            Long organizationId,
            String startDateValue,
            String endDateValue,
            String userName
    ) {
        LocalDate startDate = parseDate(startDateValue);
        LocalDate endDate = parseDate(endDateValue);
        validateRange(startDate, endDate);

        YearMonth targetMonth = YearMonth.from(startDate);
        YearMonth previousMonth = targetMonth.minusMonths(1);
        YearMonth nextMonth = targetMonth.plusMonths(1);
        boolean hasPrev = hasSetting(organizationId, previousMonth);
        boolean hasNext = hasSetting(organizationId, nextMonth);

        Optional<WorkScheduleSetting> settingOptional = settingRepository
                .findByOrganizationIdAndYearAndMonth(
                        organizationId, startDate.getYear(), startDate.getMonthValue()
                );
        if (settingOptional.isEmpty()) {
            return new AdminScheduleRangeResponse(
                    startDate,
                    endDate,
                    DEFAULT_MAX_CONCURRENT_WORKERS,
                    hasPrev,
                    hasNext,
                    List.of()
            );
        }
        WorkScheduleSetting setting = settingOptional.get();

        List<WorkSchedule> schedules =
                scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                        setting, startDate, endDate, ACTIVE_STATUSES
                );
        List<WorkUnavailableTime> unavailableTimes =
                unavailableTimeRepository.findBySettingAndDateBetween(setting, startDate, endDate);

        List<WorkAttendance> attendances = schedules.isEmpty()
                ? List.of()
                : attendanceRepository.findAllByScheduleIn(schedules);
        Map<Long, List<WorkAttendance>> attendancesByScheduleId = attendances.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        attendance -> attendance.getSchedule().getScheduleId()
                ));

        Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> workersBySlot =
                buildWorkersBySlot(schedules, attendancesByScheduleId, LocalDateTime.now());
        Set<SlotKey> unavailableSlots = WorkSlotUtils.buildUnavailableSlotKeys(
                unavailableTimes, WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES);

        Set<SlotKey> visibleSlots = buildAllSlots(startDate, endDate);

        String keyword = userName == null ? "" : userName.trim().toLowerCase(Locale.ROOT);
        List<AdminScheduleRangeResponse.Day> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            List<AdminScheduleRangeResponse.Slot> slots = visibleSlots.stream()
                    .filter(slot -> slot.date().equals(currentDate))
                    .filter(slot -> matchesUserName(slot, workersBySlot, keyword))
                    .sorted(Comparator.comparing(SlotKey::start))
                    .map(slot -> toResponseSlot(
                            slot, workersBySlot, unavailableSlots, setting.getMaxConcurrentWorkers()
                    ))
                    .toList();
            days.add(new AdminScheduleRangeResponse.Day(currentDate, slots));
        }

        return new AdminScheduleRangeResponse(
                startDate,
                endDate,
                setting.getMaxConcurrentWorkers(),
                hasPrev,
                hasNext,
                days
        );
    }

    private boolean hasSetting(Long organizationId, YearMonth yearMonth) {
        return settingRepository.existsByOrganizationIdAndYearAndMonth(
                organizationId,
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );
    }

    private Set<SlotKey> buildAllSlots(LocalDate startDate, LocalDate endDate) {
        Set<SlotKey> slots = new HashSet<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            slots.addAll(WorkSlotUtils.expandToSlots(
                    date, WORK_START_TIME, WORK_END_TIME, SLOT_MINUTES
            ));
        }
        return slots;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_SCHEDULE_QUERY_INVALID);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_SCHEDULE_QUERY_INVALID);
        }
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)
                || !YearMonth.from(startDate).equals(YearMonth.from(endDate))) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_SCHEDULE_QUERY_INVALID);
        }
    }

    private Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> buildWorkersBySlot(
            List<WorkSchedule> schedules,
            Map<Long, List<WorkAttendance>> attendancesByScheduleId,
            LocalDateTime referenceTime
    ) {
        Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> result = new HashMap<>();
        schedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getDate)
                        .thenComparing(WorkSchedule::getStartTime)
                        .thenComparing(schedule -> schedule.getUser().getName()))
                .forEach(schedule -> WorkSlotUtils.expandToSlots(
                        schedule.getDate(), schedule.getStartTime(), schedule.getEndTime(), SLOT_MINUTES
                ).forEach(slot -> result.computeIfAbsent(slot, ignored -> new LinkedHashMap<>())
                        .putIfAbsent(
                                schedule.getUser().getUserId(),
                                new AdminScheduleRangeResponse.Worker(
                                        String.valueOf(schedule.getUser().getUserId()),
                                        schedule.getUser().getName(),
                                        schedule.getScheduleId(),
                                        resolveWorkStatus(
                                                schedule,
                                                attendancesByScheduleId.getOrDefault(
                                                        schedule.getScheduleId(), List.of()
                                                ),
                                                referenceTime
                                        )
                                )
                        )));
        return result;
    }

    private String resolveWorkStatus(
            WorkSchedule schedule,
            List<WorkAttendance> attendances,
            LocalDateTime referenceTime
    ) {
        boolean checkedOut = attendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT02);
        if (checkedOut) {
            return CodeType.WK03.name();
        }

        boolean checkedIn = attendances.stream()
                .anyMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT01);
        if (checkedIn) {
            return CodeType.WK02.name();
        }

        LocalDateTime scheduledEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
        return referenceTime.isAfter(scheduledEnd)
                ? CodeType.WK04.name()
                : CodeType.WK01.name();
    }

    private boolean matchesUserName(
            SlotKey slot,
            Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> workersBySlot,
            String keyword
    ) {
        if (keyword.isEmpty()) {
            return true;
        }
        return workersBySlot.getOrDefault(slot, new LinkedHashMap<>()).values().stream()
                .anyMatch(worker -> worker.userName().toLowerCase(Locale.ROOT).contains(keyword));
    }

    private AdminScheduleRangeResponse.Slot toResponseSlot(
            SlotKey slot,
            Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> workersBySlot,
            Set<SlotKey> unavailableSlots,
            int maxConcurrentWorkers
    ) {
        List<AdminScheduleRangeResponse.Worker> workers = List.copyOf(
                workersBySlot.getOrDefault(slot, new LinkedHashMap<>()).values()
        );
        int currentCount = workers.size();
        boolean unavailable = unavailableSlots.contains(slot);
        return new AdminScheduleRangeResponse.Slot(
                slot.start(),
                slot.end(),
                unavailable ? "UNAVAILABLE" : "AVAILABLE",
                currentCount,
                currentCount > maxConcurrentWorkers,
                workers
        );
    }

}
