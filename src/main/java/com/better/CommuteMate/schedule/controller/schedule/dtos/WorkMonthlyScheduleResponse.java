package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
public class WorkMonthlyScheduleResponse extends ResponseDetail {

    private final Integer year;
    private final Integer month;
    private final Integer maxConcurrentWorkers;
    private final Integer totalLimitHours;
    private final Integer usedHours;
    private final List<DaySchedule> days;

    @Builder
    public WorkMonthlyScheduleResponse(
            Integer year,
            Integer month,
            Integer maxConcurrentWorkers,
            Integer totalLimitHours,
            Integer usedHours,
            List<DaySchedule> days
    ) {
        this.year = year;
        this.month = month;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.totalLimitHours = totalLimitHours;
        this.usedHours = usedHours;
        this.days = days;
    }

    @Getter
    @Builder
    public static class DaySchedule {
        private final LocalDate date;
        private final List<SlotInfo> slots;
    }

    @Getter
    @Builder
    public static class SlotInfo {
        @JsonFormat(pattern = "HH:mm")
        private final LocalTime start;
        @JsonFormat(pattern = "HH:mm")
        private final LocalTime end;
        private final String status;
        private final Integer currentCount;
    }
}
