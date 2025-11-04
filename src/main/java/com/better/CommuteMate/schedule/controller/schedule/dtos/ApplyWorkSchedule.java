package com.better.CommuteMate.controller.schedule.dtos;

import java.util.List;

public record ApplyWorkSchedule(
        List<WorkScheduleDTO> slots
) {
}
