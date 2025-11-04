package com.better.CommuteMate.schedule.controller.admin.dtos;

public record SetMonthlyLimitRequest(
        Integer scheduleYear,
        Integer scheduleMonth,
        Integer maxConcurrent
) {
}