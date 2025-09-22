package com.better.CommuteMate.controller.schedule.dtos;

import java.time.LocalDateTime;

public record WorkScheduleDTO(
        LocalDateTime start,
        LocalDateTime end
) {
}
