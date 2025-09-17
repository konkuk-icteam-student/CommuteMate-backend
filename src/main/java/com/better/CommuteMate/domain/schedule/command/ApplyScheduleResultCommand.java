package com.better.CommuteMate.domain.schedule.command;

import com.better.CommuteMate.controller.schedule.dtos.WorkScheduleDTO;

import java.util.List;

public record ApplyScheduleResultCommand(
        List<WorkScheduleDTO> success,
        List<WorkScheduleDTO> fail
) {
}
