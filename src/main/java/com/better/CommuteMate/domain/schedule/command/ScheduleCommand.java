package com.better.CommuteMate.domain.schedule.command;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleCommand(
        String email,
        LocalDate date,
        LocalTime start,
        LocalTime end) {
}
