package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;

import java.util.ArrayList;
import java.util.List;

public class ScheduleSlotUtils {

    private ScheduleSlotUtils() {}

    /**
     * start_time 오름차순으로 정렬된 슬롯 목록을 연속된 그룹으로 병합한다.
     * 앞 슬롯의 end_time == 뒤 슬롯의 start_time 일 때만 이어붙인다.
     */
    public static List<List<WorkSchedule>> mergeConsecutiveSlots(List<WorkSchedule> sortedSlots) {
        List<List<WorkSchedule>> groups = new ArrayList<>();
        if (sortedSlots.isEmpty()) {
            return groups;
        }
        List<WorkSchedule> current = new ArrayList<>();
        current.add(sortedSlots.get(0));
        for (int i = 1; i < sortedSlots.size(); i++) {
            WorkSchedule prev = sortedSlots.get(i - 1);
            WorkSchedule next = sortedSlots.get(i);
            if (prev.getEndTime().equals(next.getStartTime())) {
                current.add(next);
            } else {
                groups.add(new ArrayList<>(current));
                current = new ArrayList<>();
                current.add(next);
            }
        }
        groups.add(current);
        return groups;
    }

    /**
     * start_time 오름차순으로 정렬된 슬롯 목록이 끊김 없이 연속되는지 검사한다.
     * 단일 슬롯은 항상 true.
     */
    public static boolean isConsecutive(List<WorkSchedule> sortedSlots) {
        for (int i = 1; i < sortedSlots.size(); i++) {
            if (!sortedSlots.get(i - 1).getEndTime().equals(sortedSlots.get(i).getStartTime())) {
                return false;
            }
        }
        return true;
    }
}
