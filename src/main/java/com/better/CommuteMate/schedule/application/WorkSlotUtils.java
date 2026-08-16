package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 근무 슬롯 분할/병합 유틸.
 * apply·edit·어드민 승인 처리에서 재사용한다.
 */
public final class WorkSlotUtils {

    private WorkSlotUtils() {}

    /** 연속 구간의 시작·끝 시각 쌍. */
    public record TimeRange(LocalTime start, LocalTime end) {}

    /** 날짜·시작·종료로 구성되는 슬롯 식별 키. */
    public record SlotKey(LocalDate date, LocalTime start, LocalTime end) {}

    /**
     * 종일 불가 여부 판정.
     * 현재 저장 규칙인 MIN~MIN과 기존 저장값인 MIN~MAX를 모두 지원한다.
     */
    public static boolean isAllDayUnavailable(WorkUnavailableTime u) {
        if (!LocalTime.MIN.equals(u.getStartTime())) {
            return false;
        }
        return LocalTime.MIN.equals(u.getEndTime())
                || u.getEndTime().toSecondOfDay() == LocalTime.MAX.toSecondOfDay();
    }

    /**
     * 지정한 시간 범위를 slotMinutes 단위 SlotKey 목록으로 분할한다.
     * endTime == LocalTime.MAX(23:59:59.999...) 이면 다음 날 자정을 상한으로
     * 처리해 LocalTime 자정 wrap-around 무한 루프를 방지한다.
     * 안전 캡: 최대 24h / slotMinutes 개.
     */
    public static List<SlotKey> expandToSlots(
            LocalDate date, LocalTime start, LocalTime end, int slotMinutes) {
        List<SlotKey> result = new ArrayList<>();
        LocalDateTime current = date.atTime(start);
        LocalDateTime limit = LocalTime.MAX.equals(end)
                ? date.plusDays(1).atStartOfDay()
                : date.atTime(end);
        int maxSlots = 24 * 60 / slotMinutes;
        for (int i = 0; current.isBefore(limit) && i < maxSlots; i++) {
            LocalDateTime next = current.plusMinutes(slotMinutes);
            result.add(new SlotKey(date, current.toLocalTime(), next.toLocalTime()));
            current = next;
        }
        return result;
    }

    /**
     * WorkUnavailableTime 목록을 SlotKey Set으로 변환한다.
     * 종일 불가(MIN~MAX)는 workStart~workEnd 범위로 확장한다.
     */
    public static Set<SlotKey> buildUnavailableSlotKeys(
            List<WorkUnavailableTime> unavailableTimes,
            LocalTime workStart,
            LocalTime workEnd,
            int slotMinutes) {
        Set<SlotKey> result = new HashSet<>();
        for (WorkUnavailableTime u : unavailableTimes) {
            LocalTime s = isAllDayUnavailable(u) ? workStart : u.getStartTime();
            LocalTime e = isAllDayUnavailable(u) ? workEnd   : u.getEndTime();
            result.addAll(expandToSlots(u.getDate(), s, e, slotMinutes));
        }
        return result;
    }

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
