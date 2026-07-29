package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleQuickSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWorkScheduleQuickSearchService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final UserRepository userRepository;
    private final WorkSchedulesRepository scheduleRepository;

    public AdminWorkScheduleQuickSearchResponse search(
            String userIdValue,
            String startDateValue,
            String endDateValue,
            Long organizationId
    ) {
        User user = findUser(userIdValue, organizationId);
        DateRange range = parseRange(startDateValue, endDateValue);

        List<WorkSchedule> schedules =
                scheduleRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        user.getUserId(),
                        range.startDate(),
                        range.endDate(),
                        List.of(CodeType.WS02)
                );

        Map<LocalDate, List<WorkSchedule>> schedulesByDate = new LinkedHashMap<>();
        schedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getDate)
                        .thenComparing(WorkSchedule::getStartTime)
                        .thenComparing(WorkSchedule::getEndTime))
                .forEach(schedule -> schedulesByDate
                        .computeIfAbsent(schedule.getDate(), ignored -> new ArrayList<>())
                        .add(schedule));

        List<AdminWorkScheduleQuickSearchResponse.Day> days = schedulesByDate.entrySet().stream()
                .map(entry -> new AdminWorkScheduleQuickSearchResponse.Day(
                        entry.getKey(),
                        koreanDayOfWeek(entry.getKey().getDayOfWeek()),
                        mergeSlots(entry.getValue())
                ))
                .toList();

        return new AdminWorkScheduleQuickSearchResponse(
                String.valueOf(user.getUserId()),
                user.getName(),
                days
        );
    }

    private User findUser(String userIdValue, Long organizationId) {
        try {
            return userRepository
                    .findByUserIdAndOrganizationId(Long.parseLong(userIdValue), organizationId)
                    .orElseThrow(() -> CustomException.of(
                            ScheduleErrorCode.ADMIN_WORK_SCHEDULE_USER_NOT_FOUND
                    ));
        } catch (NumberFormatException e) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_WORK_SCHEDULE_USER_NOT_FOUND);
        }
    }

    private DateRange parseRange(String startDateValue, String endDateValue) {
        if (startDateValue == null || startDateValue.isBlank()
                || endDateValue == null || endDateValue.isBlank()) {
            throw invalidRange();
        }
        try {
            LocalDate startDate = LocalDate.parse(startDateValue, DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(endDateValue, DATE_FORMATTER);
            if (startDate.isAfter(endDate)) {
                throw invalidRange();
            }
            return new DateRange(startDate, endDate);
        } catch (DateTimeParseException e) {
            throw invalidRange();
        }
    }

    private List<AdminWorkScheduleQuickSearchResponse.Slot> mergeSlots(
            List<WorkSchedule> schedules
    ) {
        List<AdminWorkScheduleQuickSearchResponse.Slot> merged = new ArrayList<>();
        LocalTime currentStart = null;
        LocalTime currentEnd = null;

        for (WorkSchedule schedule : schedules) {
            if (currentStart == null) {
                currentStart = schedule.getStartTime();
                currentEnd = schedule.getEndTime();
                continue;
            }
            if (!schedule.getStartTime().isAfter(currentEnd)) {
                if (schedule.getEndTime().isAfter(currentEnd)) {
                    currentEnd = schedule.getEndTime();
                }
                continue;
            }
            merged.add(new AdminWorkScheduleQuickSearchResponse.Slot(
                    currentStart, currentEnd
            ));
            currentStart = schedule.getStartTime();
            currentEnd = schedule.getEndTime();
        }

        if (currentStart != null) {
            merged.add(new AdminWorkScheduleQuickSearchResponse.Slot(
                    currentStart, currentEnd
            ));
        }
        return merged;
    }

    private String koreanDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private CustomException invalidRange() {
        return CustomException.of(ScheduleErrorCode.ADMIN_WORK_SCHEDULE_INVALID_RANGE);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
