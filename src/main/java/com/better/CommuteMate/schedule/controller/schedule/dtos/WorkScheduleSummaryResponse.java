package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class WorkScheduleSummaryResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate endDate;
    @Schema(description = "근무 신청 최소 단위 (분)", example = "30")
    private final int minWorkUnitMinutes;
    private final PeriodSummary week;
    private final PeriodSummary month;

    @Builder
    public WorkScheduleSummaryResponse(
            LocalDate startDate,
            LocalDate endDate,
            int minWorkUnitMinutes,
            PeriodSummary week,
            PeriodSummary month
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.minWorkUnitMinutes = minWorkUnitMinutes;
        this.week = week;
        this.month = month;
    }

    @Getter
    @Builder
    public static class PeriodSummary {
        @Schema(description = "기간 레이블", example = "1주차")
        private final String label;
        @Schema(description = "실제 사용 시간 (시간)", example = "3")
        private final int usedHours;
        @Schema(description = "최소 근무 시간 (시간)", example = "5")
        private final int minHours;
        @Schema(description = "최대 근무 시간 (시간)", example = "13")
        private final int maxHours;
    }
}
