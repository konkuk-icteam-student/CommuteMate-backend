package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class TodayScheduleResponse extends ResponseDetail {

    @Schema(description = "조회 날짜", example = "2026-10-13")
    private LocalDate date;

    @Schema(description = "오늘 근무 일정 목록")
    private List<ScheduleItem> schedules;

    @Builder
    public TodayScheduleResponse(LocalDate date, List<ScheduleItem> schedules) {
        super();
        this.date = date;
        this.schedules = schedules;
    }

    @Getter
    @Builder
    public static class ScheduleItem {

        @Schema(description = "병합된 근무 슬롯 ID 목록 (시간 순)", example = "[1, 2, 3, 4, 5]")
        private List<Long> scheduleIds;

        @Schema(description = "근무 구분 라벨 (오전 12시 기준 오전/오후)", example = "오전 근무")
        private String label;

        @Schema(description = "근무 시작 시각", example = "09:30")
        private LocalTime start;

        @Schema(description = "근무 종료 시각", example = "11:30")
        private LocalTime end;

        @Schema(description = "근무 상태 코드 (WK01: 예정, WK02: 근무중, WK03: 근무완료, WK04: 결근)", example = "WK02")
        private String workStatusCode;

        @Schema(description = "출근 여부", example = "true")
        private boolean checkedIn;

        @Schema(description = "출근 시각. 출근 전이면 null.", example = "2026-10-13T09:02:00", nullable = true)
        private LocalDateTime checkInTime;
    }
}
