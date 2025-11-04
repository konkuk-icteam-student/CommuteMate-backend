package com.better.CommuteMate.schedule.controller.dtos;

public record SetMonthlyLimitRequest(
        Integer scheduleYear,
        Integer scheduleMonth,
        Integer maxConcurrent
) {
}