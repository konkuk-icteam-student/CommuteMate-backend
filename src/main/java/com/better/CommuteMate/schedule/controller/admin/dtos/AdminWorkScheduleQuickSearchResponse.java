package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AdminWorkScheduleQuickSearchResponse extends ResponseDetail {

    public final String userId;
    public final String userName;
    public final List<Day> days;

    public AdminWorkScheduleQuickSearchResponse(
            String userId,
            String userName,
            List<Day> days
    ) {
        this.userId = userId;
        this.userName = userName;
        this.days = days;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record Day(
            LocalDate date,
            String dayOfWeek,
            List<Slot> slots
    ) {
    }

    public record Slot(
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end
    ) {
    }
}
