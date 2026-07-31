package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class AdminWorkAssignmentResponse extends ResponseDetail {

    private final String scheduleId;
    private final String userId;
    private final String userName;
    private final LocalDate date;
    @JsonFormat(pattern = "HH:mm")
    private final LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private final LocalTime endTime;
    private final long currentCount;
    private final int maxConcurrentWorkers;

    public AdminWorkAssignmentResponse(
            String scheduleId,
            String userId,
            String userName,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            long currentCount,
            int maxConcurrentWorkers
    ) {
        this.scheduleId = scheduleId;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentCount = currentCount;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
