package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SaveScheduleSettingRequest(
        @Schema(type = "string", format = "date", example = "2026-04-01")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate applyStartDate,

        @Schema(type = "string", format = "date", example = "2026-04-10")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate applyEndDate,

        @Schema(example = "[\"2026-04-19\"]")
        List<@NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate> unavailableDates,

        @Schema(example = "[{\"start\":\"11:00\",\"end\":\"13:00\"}]")
        List<@NotNull(message = "요청 값이 올바르지 않습니다.") @Valid UnavailableTimeRange> unavailableTimeRanges,

        @Schema(example = "4")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 1, message = "요청 값이 올바르지 않습니다.") Integer maxConcurrentWorkers,

        @Schema(example = "120")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 1, message = "요청 값이 올바르지 않습니다.") Integer minWorkUnitMinutes,

        @Schema(example = "300")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer weeklyMinMinutes,

        @Schema(example = "540")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer weeklyMaxMinutes,

        @Schema(example = "1200")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer monthlyMinMinutes,

        @Schema(example = "1620")
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer monthlyMaxMinutes
) {
    public List<LocalDate> unavailableDatesOrEmpty() {
        return unavailableDates == null ? List.of() : unavailableDates;
    }

    public List<UnavailableTimeRange> unavailableTimeRangesOrEmpty() {
        return unavailableTimeRanges == null ? List.of() : unavailableTimeRanges;
    }

    public record UnavailableTimeRange(
            @Schema(type = "string", format = "time", example = "11:00")
            @NotNull(message = "요청 값이 올바르지 않습니다.")
            @JsonFormat(pattern = "HH:mm") LocalTime start,

            @Schema(type = "string", format = "time", example = "13:00")
            @NotNull(message = "요청 값이 올바르지 않습니다.")
            @JsonFormat(pattern = "HH:mm") LocalTime end
    ) {
    }
}
