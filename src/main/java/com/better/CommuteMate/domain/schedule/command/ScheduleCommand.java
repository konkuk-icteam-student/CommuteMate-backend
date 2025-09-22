package com.better.CommuteMate.domain.schedule.command;

import java.time.LocalDateTime;

public record ScheduleCommand(
        String email,
        LocalDateTime start,
        LocalDateTime end) {
}
