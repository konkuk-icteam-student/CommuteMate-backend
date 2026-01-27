package com.better.CommuteMate.schedule.application.dtos;

import java.time.LocalDateTime;

public record SetApplyTermCommand(
        Integer scheduleYear,
        Integer scheduleMonth,
        LocalDateTime applyStartTime,
        LocalDateTime applyEndTime,
        Long userId
) {
    public static SetApplyTermCommand from(
            Integer scheduleYear,
            Integer scheduleMonth,
            LocalDateTime applyStartTime,
            LocalDateTime applyEndTime,
            Long userId) {
        return new SetApplyTermCommand(
                scheduleYear,
                scheduleMonth,
                applyStartTime,
                applyEndTime,
                userId
        );
    }
}
