package com.better.CommuteMate.schedule.application.dtos;

public record MonthlyScheduleLimitCommand(
        Integer scheduleYear,
        Integer scheduleMonth,
        Integer maxConcurrent,
        Integer userId) {
    public static MonthlyScheduleLimitCommand from(
            Integer scheduleYear,
            Integer scheduleMonth,
            Integer maxConcurrent,
            Integer userId) {
        return new MonthlyScheduleLimitCommand(
                scheduleYear,
                scheduleMonth, 
                maxConcurrent,
                userId);
    }
}
