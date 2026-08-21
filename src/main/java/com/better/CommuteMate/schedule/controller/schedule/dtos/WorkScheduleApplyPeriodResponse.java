package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class WorkScheduleApplyPeriodResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate applyStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate applyEndDate;

    private final Boolean isApplyAvailable;

    private final Boolean isEditAvailable;

    @Builder
    public WorkScheduleApplyPeriodResponse(
            LocalDate applyStartDate,
            LocalDate applyEndDate,
            Boolean isApplyAvailable,
            Boolean isEditAvailable
    ) {
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.isApplyAvailable = isApplyAvailable;
        this.isEditAvailable = isEditAvailable;
    }
}
