package com.better.CommuteMate.controller.admin.dtos;

public record SetMonthlyLimitRequest(
        Integer scheduleYear,
        Integer scheduleMonth,
        Integer maxConcurrent
) {
}