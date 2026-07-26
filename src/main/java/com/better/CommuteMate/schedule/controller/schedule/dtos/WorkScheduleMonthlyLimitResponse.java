package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkScheduleMonthlyLimitResponse extends ResponseDetail {

    private final Integer scheduleYear;
    private final Integer scheduleMonth;
    private final Integer maxConcurrentWorkers;

    @Builder
    public WorkScheduleMonthlyLimitResponse(Integer scheduleYear, Integer scheduleMonth, Integer maxConcurrentWorkers) {
        this.scheduleYear = scheduleYear;
        this.scheduleMonth = scheduleMonth;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
    }
}
