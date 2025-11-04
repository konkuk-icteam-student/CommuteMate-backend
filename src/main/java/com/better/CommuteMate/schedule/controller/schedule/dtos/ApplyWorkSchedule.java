package com.better.CommuteMate.schedule.controller.schedule.dtos;

import java.util.List;

public record ApplyWorkSchedule(
        List<WorkScheduleDTO> slots
) {
}
