package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 근무 슬롯 분할 유틸.
 * apply·edit·어드민 승인 처리에서 재사용한다.
 */
public final class WorkSlotUtils {

    private WorkSlotUtils() {}

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
