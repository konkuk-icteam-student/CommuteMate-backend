package com.better.CommuteMate.schedule.controller.dtos;

import java.util.List;

public record ApplyWorkSchedule(
        List<WorkScheduleDTO> slots
) {
}
