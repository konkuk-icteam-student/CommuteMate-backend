package com.better.CommuteMate.notification.application.dtos;

import com.better.CommuteMate.global.code.CodeType;

import java.time.LocalDate;
import java.time.LocalTime;

public record NotificationChangeItem(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        long durationMinutes,
        CodeType changeTypeCode
) {
}
