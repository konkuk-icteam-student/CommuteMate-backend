package com.better.CommuteMate.schedule.application.dtos;

import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;

import java.util.List;

public record ApplyScheduleResultCommand(
        List<WorkScheduleDTO> success,
        List<WorkScheduleDTO> fail
) {
    public static ApplyScheduleResultCommand from(List<WorkScheduleDTO> success, List<WorkScheduleDTO> fail) {
        return new ApplyScheduleResultCommand(success, fail);
    }
}
