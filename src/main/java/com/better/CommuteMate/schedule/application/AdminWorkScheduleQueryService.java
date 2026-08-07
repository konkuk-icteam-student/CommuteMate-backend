package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
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

    private final WorkScheduleSettingRepository settingRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkUnavailableTimeRepository unavailableTimeRepository;

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

        Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> workersBySlot =
                buildWorkersBySlot(schedules);
        Set<SlotKey> unavailableSlots = buildUnavailableSlots(unavailableTimes);

        Set<SlotKey> visibleSlots = new HashSet<>(workersBySlot.keySet());
        visibleSlots.addAll(unavailableSlots);

        String keyword = userName == null ? "" : userName.trim().toLowerCase(Locale.ROOT);
        List<AdminScheduleRangeResponse.Day> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            List<AdminScheduleRangeResponse.Slot> slots = visibleSlots.stream()
                    .filter(slot -> slot.date.equals(currentDate))
                    .filter(slot -> matchesUserName(slot, workersBySlot, keyword))
                    .sorted(Comparator.comparing(slot -> slot.start))
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
            List<WorkSchedule> schedules
    ) {
        Map<SlotKey, LinkedHashMap<Long, AdminScheduleRangeResponse.Worker>> result = new HashMap<>();
        schedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getDate)
                        .thenComparing(WorkSchedule::getStartTime)
                        .thenComparing(schedule -> schedule.getUser().getName()))
                .forEach(schedule -> expandToSlots(
                        schedule.getDate(), schedule.getStartTime(), schedule.getEndTime()
                ).forEach(slot -> result.computeIfAbsent(slot, ignored -> new LinkedHashMap<>())
                        .putIfAbsent(
                                schedule.getUser().getUserId(),
                                new AdminScheduleRangeResponse.Worker(
                                        String.valueOf(schedule.getUser().getUserId()),
                                        schedule.getUser().getName()
                                )
                        )));
        return result;
    }

    private Set<SlotKey> buildUnavailableSlots(List<WorkUnavailableTime> unavailableTimes) {
        Set<SlotKey> result = new HashSet<>();
        unavailableTimes.forEach(unavailable -> result.addAll(expandToSlots(
                unavailable.getDate(), unavailable.getStartTime(), unavailable.getEndTime()
        )));
        return result;
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
                slot.start,
                slot.end,
                unavailable ? "UNAVAILABLE" : "AVAILABLE",
                currentCount,
                currentCount > maxConcurrentWorkers,
                workers
        );
    }

    private List<SlotKey> expandToSlots(LocalDate date, LocalTime start, LocalTime end) {
        List<SlotKey> result = new ArrayList<>();
        LocalDateTime current = date.atTime(start);
        LocalDateTime limit = end.equals(LocalTime.MAX)
                ? date.plusDays(1).atStartOfDay()
                : date.atTime(end);
        int maximumSlots = 24 * 60 / SLOT_MINUTES;
        for (int index = 0; current.isBefore(limit) && index < maximumSlots; index++) {
            LocalDateTime next = current.plusMinutes(SLOT_MINUTES);
            result.add(new SlotKey(date, current.toLocalTime(), next.toLocalTime()));
            current = next;
        }
        return result;
    }

    private record SlotKey(LocalDate date, LocalTime start, LocalTime end) {
    }
}
