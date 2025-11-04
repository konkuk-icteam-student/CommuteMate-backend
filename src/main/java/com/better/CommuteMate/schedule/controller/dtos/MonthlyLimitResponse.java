package com.better.CommuteMate.schedule.controller.dtos;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleLimit;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MonthlyLimitResponse extends ResponseDetail {
    private final Integer scheduleYear;
    private final Integer scheduleMonth;
    private final Integer maxConcurrent;

    public static MonthlyLimitResponse from(MonthlyScheduleLimit limit) {
        return MonthlyLimitResponse.builder()
                .scheduleYear(limit.getScheduleYear())
                .scheduleMonth(limit.getScheduleMonth())
                .maxConcurrent(limit.getMaxConcurrent())
                .build();
    }
}