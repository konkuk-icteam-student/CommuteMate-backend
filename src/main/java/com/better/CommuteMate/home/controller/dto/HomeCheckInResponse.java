package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class HomeCheckInResponse extends ResponseDetail {

    @Schema(description = "출근 처리된 슬롯 ID 목록", example = "[1, 2, 3, 4, 5]")
    private List<Long> scheduleIds;

    @Schema(description = "출근 시각", example = "2026-10-13T09:02:00")
    private LocalDateTime checkInTime;

    @Builder
    public HomeCheckInResponse(List<Long> scheduleIds, LocalDateTime checkInTime) {
        super();
        this.scheduleIds = scheduleIds;
        this.checkInTime = checkInTime;
    }
}
