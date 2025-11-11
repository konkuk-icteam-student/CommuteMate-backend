package com.better.CommuteMate.schedule.controller.admin.dtos;

import java.time.LocalDateTime;

public record SetApplyTermRequest(
        Integer scheduleYear,
        Integer scheduleMonth,
        LocalDateTime applyStartTime,
        LocalDateTime applyEndTime
) {
}
