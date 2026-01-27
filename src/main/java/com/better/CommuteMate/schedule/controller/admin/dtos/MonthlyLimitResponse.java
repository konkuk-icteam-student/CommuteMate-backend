package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
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

    public static MonthlyLimitResponse from(MonthlyScheduleConfig limit) {
        return MonthlyLimitResponse.builder()
                .scheduleYear(limit.getScheduleYear())
                .scheduleMonth(limit.getScheduleMonth())
                .maxConcurrent(limit.getMaxConcurrent())
                .build();
    }
}