package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class WorkScheduleSummaryResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate endDate;
    private final PeriodSummary week;
    private final PeriodSummary month;

    @Builder
    public WorkScheduleSummaryResponse(
            LocalDate startDate,
            LocalDate endDate,
            PeriodSummary week,
            PeriodSummary month
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.week = week;
        this.month = month;
    }

    @Getter
    @Builder
    public static class PeriodSummary {
        private final String label;
        private final Integer usedHours;
        // monthly_required_minutes 컬럼명은 'required'지만, 프론트 진행률 표시 통일을 위해 'limitHours'로 응답
        private final Integer limitHours;
    }
}
