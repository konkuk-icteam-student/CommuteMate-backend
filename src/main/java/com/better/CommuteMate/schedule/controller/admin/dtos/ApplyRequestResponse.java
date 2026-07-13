package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ApplyRequestResponse(
        String scheduleId,
        Long userId,
        String userName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
    public static ApplyRequestResponse from(WorkSchedule schedule) {
        return new ApplyRequestResponse(
                schedule.getScheduleId(),
                schedule.getUser().getUserId(),
                schedule.getUser().getName(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}