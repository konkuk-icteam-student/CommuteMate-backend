package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    public final List<Day> days;

    public AdminScheduleRangeResponse(
            LocalDate startDate,
            LocalDate endDate,
            int maxConcurrentWorkers,
            List<Day> days
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
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
            String userName
    ) {
    }
}
