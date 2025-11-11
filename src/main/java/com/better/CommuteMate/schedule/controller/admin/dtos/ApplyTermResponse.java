package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ApplyTermResponse extends ResponseDetail {
    private final Integer scheduleYear;
    private final Integer scheduleMonth;
    private final LocalDateTime applyStartTime;
    private final LocalDateTime applyEndTime;

    public static ApplyTermResponse from(MonthlyScheduleConfig config) {
        return ApplyTermResponse.builder()
                .scheduleYear(config.getScheduleYear())
                .scheduleMonth(config.getScheduleMonth())
                .applyStartTime(config.getApplyStartTime())
                .applyEndTime(config.getApplyEndTime())
                .build();
    }
}
