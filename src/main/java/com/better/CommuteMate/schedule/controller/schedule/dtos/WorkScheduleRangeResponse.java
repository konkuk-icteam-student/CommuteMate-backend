package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class WorkScheduleRangeResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate endDate;
    private final Integer maxConcurrentWorkers;
    private final Integer totalLimitHours;
    private final Integer usedHours;
    private final List<WorkMonthlyScheduleResponse.DaySchedule> days;

    @Builder
    public WorkScheduleRangeResponse(
            LocalDate startDate,
            LocalDate endDate,
            Integer maxConcurrentWorkers,
            Integer totalLimitHours,
            Integer usedHours,
            List<WorkMonthlyScheduleResponse.DaySchedule> days
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.totalLimitHours = totalLimitHours;
        this.usedHours = usedHours;
        this.days = days;
    }
}
