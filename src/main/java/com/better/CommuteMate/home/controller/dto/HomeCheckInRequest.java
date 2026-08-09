package com.better.CommuteMate.home.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class HomeCheckInRequest {

    @NotEmpty(message = "출근할 근무 일정을 선택해야 합니다.")
    @Schema(description = "출근할 병합 근무의 슬롯 ID 목록 (조회 API의 scheduleIds를 그대로 전달)",
            example = "[1, 2, 3, 4, 5]")
    private List<Long> scheduleIds;
}
