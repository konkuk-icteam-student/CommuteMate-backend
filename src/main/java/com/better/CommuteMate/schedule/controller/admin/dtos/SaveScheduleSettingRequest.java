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
        @NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate applyStartDate,
        @NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate applyEndDate,
        List<@NotNull(message = "요청 값이 올바르지 않습니다.") LocalDate> unavailableDates,
        List<@NotNull(message = "요청 값이 올바르지 않습니다.") @Valid UnavailableTimeRange> unavailableTimeRanges,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 1, message = "요청 값이 올바르지 않습니다.") Integer maxConcurrentWorkers,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 1, message = "요청 값이 올바르지 않습니다.") Integer minWorkUnitMinutes,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer weeklyMinMinutes,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer weeklyMaxMinutes,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer monthlyMinMinutes,
        @NotNull(message = "요청 값이 올바르지 않습니다.") @Min(value = 0, message = "요청 값이 올바르지 않습니다.") Integer monthlyMaxMinutes
) {
    public List<LocalDate> unavailableDatesOrEmpty() {
        return unavailableDates == null ? List.of() : unavailableDates;
    }

    public List<UnavailableTimeRange> unavailableTimeRangesOrEmpty() {
        return unavailableTimeRanges == null ? List.of() : unavailableTimeRanges;
    }

    public record UnavailableTimeRange(
            @NotNull(message = "요청 값이 올바르지 않습니다.")
            @JsonFormat(pattern = "HH:mm")
            @Schema(type = "string", format = "time", example = "11:00") LocalTime start,
            @NotNull(message = "요청 값이 올바르지 않습니다.")
            @JsonFormat(pattern = "HH:mm")
            @Schema(type = "string", format = "time", example = "13:00") LocalTime end
    ) {
    }
}
