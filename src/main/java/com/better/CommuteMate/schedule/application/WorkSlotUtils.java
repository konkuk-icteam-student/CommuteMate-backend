package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 근무 슬롯 분할/병합 유틸.
 * apply·edit·어드민 승인 처리에서 재사용한다.
 */
public final class WorkSlotUtils {

    private WorkSlotUtils() {}

    /** 연속 구간의 시작·끝 시각 쌍. */
    public record TimeRange(LocalTime start, LocalTime end) {}

    /**
     * 같은 날의 슬롯 목록을 시작 시각 순으로 정렬하고,
     * end == next.start 인 인접 슬롯을 하나의 연속 구간으로 병합해 반환한다.
     * 호출 전에 날짜별로 분리된 목록을 전달해야 한다.
     */
    public static List<TimeRange> mergeConsecutiveRanges(List<WorkScheduleSlotCommand> slots) {
        if (slots.isEmpty()) return List.of();
        List<WorkScheduleSlotCommand> sorted = slots.stream()
                .sorted(Comparator.comparing(WorkScheduleSlotCommand::start))
                .toList();
        List<TimeRange> result = new ArrayList<>();
        LocalTime rangeStart = sorted.get(0).start();
        LocalTime rangeEnd = sorted.get(0).end();
        for (int i = 1; i < sorted.size(); i++) {
            WorkScheduleSlotCommand s = sorted.get(i);
            if (s.start().equals(rangeEnd)) {
                rangeEnd = s.end();
            } else {
                result.add(new TimeRange(rangeStart, rangeEnd));
                rangeStart = s.start();
                rangeEnd = s.end();
            }
        }
        result.add(new TimeRange(rangeStart, rangeEnd));
        return result;
    }

    /**
     * 지정한 시간 범위를 unitMinutes 단위로 분할해 단위 슬롯 목록을 반환한다.
     * 예: 09:00~11:00, unitMinutes=30 → [09:00~09:30, 09:30~10:00, 10:00~10:30, 10:30~11:00]
     */
    public static List<WorkScheduleSlotCommand> splitIntoUnitSlots(
            LocalDate date, LocalTime start, LocalTime end, int unitMinutes) {
        List<WorkScheduleSlotCommand> slots = new ArrayList<>();
        LocalTime current = start;
        while (current.isBefore(end)) {
            LocalTime next = current.plusMinutes(unitMinutes);
            slots.add(new WorkScheduleSlotCommand(date, current, next));
            current = next;
        }
        return slots;
    }
}
