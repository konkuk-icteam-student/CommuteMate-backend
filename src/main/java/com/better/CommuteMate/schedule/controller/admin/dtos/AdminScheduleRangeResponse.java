package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AdminScheduleRangeResponse extends ResponseDetail {
    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate endDate;
    public final int maxConcurrentWorkers;
    public final boolean hasPrev;
    public final boolean hasNext;
    public final List<Day> days;

    public AdminScheduleRangeResponse(
            LocalDate startDate,
            LocalDate endDate,
            int maxConcurrentWorkers,
            boolean hasPrev,
            boolean hasNext,
            List<Day> days
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.hasPrev = hasPrev;
        this.hasNext = hasNext;
        this.days = days;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record Day(
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            List<Slot> slots
    ) {
    }

    public record Slot(
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end,
            String status,
            int currentCount,
            boolean isOverLimit,
            List<Worker> users
    ) {
    }

    public record Worker(
            String userId,
            String userName,
            @Schema(
                    description = """
                            삭제 API(DELETE /api/v1/admin/work-schedules/{scheduleId})에 사용할 근무 신청 ID.
                            같은 WorkSchedule(예: 09:00~11:00)은 30분 슬롯 단위로 분해되므로,
                            연속된 여러 슬롯에 동일한 scheduleId가 반복 노출됩니다.
                            이 scheduleId로 삭제 API를 1회 호출하면 해당 WorkSchedule 전체(연결된 모든 슬롯)가 취소됩니다.
                            """,
                    example = "101"
            )
            Long scheduleId,
            @Schema(
                    description = "근태 상태 코드: WK01 근무 예정, WK02 근무 중, WK03 근무 완료, WK04 미출근",
                    example = "WK03"
            )
            String workStatusCode
    ) {
    }
}
