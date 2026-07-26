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
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyScheduleSettingService {

    private static final List<CodeType> ACTIVE_STATUSES = List.of(CodeType.WS01, CodeType.WS02);

    private final WorkScheduleSettingRepository settingRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkUnavailableTimeRepository unavailableTimeRepository;

    @Transactional
    public SaveScheduleSettingResponse save(
            String organizationId,
            int year,
            int month,
            SaveScheduleSettingRequest request,
            String updatedBy
    ) {
        validate(year, month, request);

        WorkScheduleSetting setting = settingRepository
                .findByOrganizationIdAndYearAndMonth(organizationId, year, month)
                .orElseGet(() -> newSetting(organizationId, year, month, request, updatedBy));

        List<WorkSchedule> schedules = setting.getSettingId() == null
                ? List.of()
                : scheduleRepository.findAllBySettingAndStatusCodeIn(setting, ACTIVE_STATUSES);
        Set<WorkSchedule> affected = findAffected(schedules, request);

        setting.updateMonthlySetting(
                request.applyStartDate().atStartOfDay(),
                request.applyEndDate().atTime(LocalTime.MAX),
                request.maxConcurrentWorkers(),
                request.minWorkUnitMinutes(),
                request.weeklyMinMinutes(),
                request.weeklyMaxMinutes(),
                request.monthlyMinMinutes(),
                request.monthlyMaxMinutes(),
                updatedBy
        );
        settingRepository.save(setting);
        replaceUnavailableTimes(setting, YearMonth.of(year, month), request);

        affected.forEach(schedule -> schedule.cancel(updatedBy));
        int affectedUsers = (int) affected.stream()
                .map(schedule -> schedule.getUser().getUserId())
                .distinct()
                .count();

        return new SaveScheduleSettingResponse(
                year, month, request, affected.size(), affectedUsers
        );
    }

    private void validate(int year, int month, SaveScheduleSettingRequest request) {
        try {
            YearMonth.of(year, month);
        } catch (RuntimeException e) {
            throw CustomException.of(ScheduleErrorCode.INVALID_SCHEDULE_SETTING_REQUEST);
        }
        if (!request.applyStartDate().isBefore(request.applyEndDate())) {
            throw CustomException.of(ScheduleErrorCode.INVALID_SETTING_APPLY_DATE);
        }
        if (request.weeklyMinMinutes() > request.weeklyMaxMinutes()
                || request.monthlyMinMinutes() > request.monthlyMaxMinutes()) {
            throw CustomException.of(ScheduleErrorCode.INVALID_SETTING_MIN_MAX);
        }
        YearMonth target = YearMonth.of(year, month);
        boolean invalidDate = request.unavailableDatesOrEmpty().stream()
                .anyMatch(date -> !YearMonth.from(date).equals(target));
        boolean invalidTime = request.unavailableTimeRangesOrEmpty().stream()
                .anyMatch(range -> !range.start().isBefore(range.end()));
        if (invalidDate || invalidTime) {
            throw CustomException.of(ScheduleErrorCode.INVALID_SCHEDULE_SETTING_REQUEST);
        }
    }

    private WorkScheduleSetting newSetting(
            String organizationId,
            int year,
            int month,
            SaveScheduleSettingRequest request,
            String updatedBy
    ) {
        return WorkScheduleSetting.builder()
                .organizationId(organizationId)
                .year(year)
                .month(month)
                .applyStartAt(request.applyStartDate().atStartOfDay())
                .applyEndAt(request.applyEndDate().atTime(LocalTime.MAX))
                .maxConcurrentWorkers(request.maxConcurrentWorkers())
                .minWorkUnitMinutes(request.minWorkUnitMinutes())
                .monthlyRequiredMinutes(request.monthlyMaxMinutes())
                .monthlyMinMinutes(request.monthlyMinMinutes())
                .monthlyMaxMinutes(request.monthlyMaxMinutes())
                .weeklyMinMinutes(request.weeklyMinMinutes())
                .weeklyMaxMinutes(request.weeklyMaxMinutes())
                .createdBy(updatedBy)
                .updatedBy(updatedBy)
                .build();
    }

    private Set<WorkSchedule> findAffected(
            List<WorkSchedule> schedules,
            SaveScheduleSettingRequest request
    ) {
        Set<WorkSchedule> affected = new LinkedHashSet<>();
        Set<LocalDate> unavailableDates = new HashSet<>(request.unavailableDatesOrEmpty());

        for (WorkSchedule schedule : schedules) {
            long minutes = durationMinutes(schedule);
            boolean outsideApplyPeriod = schedule.getCreatedAt() != null
                    && (schedule.getCreatedAt().toLocalDate().isBefore(request.applyStartDate())
                    || schedule.getCreatedAt().toLocalDate().isAfter(request.applyEndDate()));
            boolean unavailable = unavailableDates.contains(schedule.getDate())
                    || request.unavailableTimeRangesOrEmpty().stream()
                    .anyMatch(range -> overlaps(
                            schedule.getStartTime(), schedule.getEndTime(), range.start(), range.end()
                    ));
            if (outsideApplyPeriod || unavailable
                    || minutes < request.minWorkUnitMinutes()
                    || minutes % request.minWorkUnitMinutes() != 0) {
                affected.add(schedule);
            }
        }

        applyConcurrentLimit(schedules, affected, request.maxConcurrentWorkers());
        applyPeriodLimits(
                schedules, affected, this::weekStart,
                request.weeklyMinMinutes(), request.weeklyMaxMinutes()
        );
        applyPeriodLimits(
                schedules, affected, schedule -> schedule.getUser().getUserId(),
                request.monthlyMinMinutes(), request.monthlyMaxMinutes()
        );
        // A monthly cancellation can make a week fall below its new minimum.
        applyMinimumLimit(schedules, affected, this::weekStart, request.weeklyMinMinutes());
        applyMinimumLimit(
                schedules, affected, schedule -> schedule.getUser().getUserId(),
                request.monthlyMinMinutes()
        );
        return affected;
    }

    private void applyConcurrentLimit(
            List<WorkSchedule> schedules,
            Set<WorkSchedule> affected,
            int limit
    ) {
        Comparator<WorkSchedule> order = Comparator
                .comparing(WorkSchedule::getDate)
                .thenComparing(WorkSchedule::getStartTime)
                .thenComparing(WorkSchedule::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(WorkSchedule::getScheduleId, Comparator.nullsFirst(Comparator.naturalOrder()));
        List<WorkSchedule> kept = new ArrayList<>();
        schedules.stream().filter(schedule -> !affected.contains(schedule)).sorted(order).forEach(schedule -> {
            if (wouldExceedConcurrentLimit(schedule, kept, limit)) {
                affected.add(schedule);
            } else {
                kept.add(schedule);
            }
        });
    }

    private boolean wouldExceedConcurrentLimit(
            WorkSchedule candidate,
            List<WorkSchedule> kept,
            int limit
    ) {
        List<WorkSchedule> sameDay = kept.stream()
                .filter(other -> other.getDate().equals(candidate.getDate()))
                .filter(other -> overlaps(
                        other.getStartTime(), other.getEndTime(),
                        candidate.getStartTime(), candidate.getEndTime()
                ))
                .toList();
        Set<LocalTime> boundaries = new TreeSet<>();
        boundaries.add(candidate.getStartTime());
        boundaries.add(candidate.getEndTime());
        sameDay.forEach(schedule -> {
            if (schedule.getStartTime().isAfter(candidate.getStartTime())) {
                boundaries.add(schedule.getStartTime());
            }
            if (schedule.getEndTime().isBefore(candidate.getEndTime())) {
                boundaries.add(schedule.getEndTime());
            }
        });
        List<LocalTime> points = new ArrayList<>(boundaries);
        for (int i = 0; i < points.size() - 1; i++) {
            LocalTime segmentStart = points.get(i);
            LocalTime segmentEnd = points.get(i + 1);
            long active = sameDay.stream()
                    .filter(schedule -> overlaps(
                            schedule.getStartTime(), schedule.getEndTime(), segmentStart, segmentEnd
                    ))
                    .count();
            if (active + 1 > limit) {
                return true;
            }
        }
        return false;
    }

    private <K> void applyPeriodLimits(
            List<WorkSchedule> schedules,
            Set<WorkSchedule> affected,
            Function<WorkSchedule, K> periodKey,
            int minimum,
            int maximum
    ) {
        Map<UserPeriod<K>, List<WorkSchedule>> groups = activeGroups(schedules, affected, periodKey);
        for (List<WorkSchedule> group : groups.values()) {
            group.sort(Comparator.comparing(WorkSchedule::getDate).thenComparing(WorkSchedule::getStartTime));
            long total = group.stream().mapToLong(this::durationMinutes).sum();
            if (total < minimum) {
                affected.addAll(group);
                continue;
            }
            long keptMinutes = 0;
            for (WorkSchedule schedule : group) {
                long duration = durationMinutes(schedule);
                if (keptMinutes + duration > maximum) {
                    affected.add(schedule);
                } else {
                    keptMinutes += duration;
                }
            }
        }
    }

    private <K> void applyMinimumLimit(
            List<WorkSchedule> schedules,
            Set<WorkSchedule> affected,
            Function<WorkSchedule, K> periodKey,
            int minimum
    ) {
        activeGroups(schedules, affected, periodKey).values().forEach(group -> {
            long total = group.stream().mapToLong(this::durationMinutes).sum();
            if (total < minimum) {
                affected.addAll(group);
            }
        });
    }

    private <K> Map<UserPeriod<K>, List<WorkSchedule>> activeGroups(
            List<WorkSchedule> schedules,
            Set<WorkSchedule> affected,
            Function<WorkSchedule, K> periodKey
    ) {
        return schedules.stream()
                .filter(schedule -> !affected.contains(schedule))
                .collect(Collectors.groupingBy(schedule ->
                        new UserPeriod<>(schedule.getUser().getUserId(), periodKey.apply(schedule))));
    }

    private LocalDate weekStart(WorkSchedule schedule) {
        return schedule.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private long durationMinutes(WorkSchedule schedule) {
        return Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
    }

    private boolean overlaps(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private void replaceUnavailableTimes(
            WorkScheduleSetting setting,
            YearMonth target,
            SaveScheduleSettingRequest request
    ) {
        unavailableTimeRepository.deleteAllBySetting(setting);
        List<WorkUnavailableTime> unavailableTimes = new ArrayList<>();
        Set<LocalDate> fullDates = new HashSet<>(request.unavailableDatesOrEmpty());

        fullDates.forEach(date -> unavailableTimes.add(WorkUnavailableTime.builder()
                .setting(setting)
                .date(date)
                .startTime(LocalTime.MIN)
                .endTime(LocalTime.MAX)
                .build()));

        for (int day = 1; day <= target.lengthOfMonth(); day++) {
            LocalDate date = target.atDay(day);
            if (fullDates.contains(date)) {
                continue;
            }
            request.unavailableTimeRangesOrEmpty().forEach(range ->
                    unavailableTimes.add(WorkUnavailableTime.builder()
                            .setting(setting)
                            .date(date)
                            .startTime(range.start())
                            .endTime(range.end())
                            .build()));
        }
        unavailableTimeRepository.saveAll(unavailableTimes);
    }

    private record UserPeriod<K>(Long userId, K period) {
    }
}
