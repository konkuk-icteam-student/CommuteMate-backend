package com.better.CommuteMate.schedule.controller.admin.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminWorkAssignmentRequest(
        @Schema(description = "배치할 사용자 ID", example = "1")
        @NotBlank(message = "요청 값이 올바르지 않습니다.")
        String userId,

        @Schema(description = "근무 날짜 (YYYY-MM-DD)", example = "2026-09-08")
        @NotBlank(message = "요청 값이 올바르지 않습니다.")
        String date,

        @Schema(description = "시작 시간 (HH:mm, 정각 또는 30분)", example = "09:00")
        @NotBlank(message = "요청 값이 올바르지 않습니다.")
        String startTime,

        @Schema(description = "종료 시간 (HH:mm, 시작 시간 + 30분)", example = "09:30")
        @NotBlank(message = "요청 값이 올바르지 않습니다.")
        String endTime
) {
}
