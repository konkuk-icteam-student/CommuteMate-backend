package com.better.CommuteMate.schedule.controller.dtos;

import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;

import java.time.LocalDateTime;

public record WorkScheduleDTO(
        LocalDateTime start,
        LocalDateTime end
) {
    public static WorkScheduleDTO from(WorkScheduleCommand workScheduleCommand) {
        return new WorkScheduleDTO(workScheduleCommand.start(), workScheduleCommand.end());
    }
    public static WorkScheduleDTO from(LocalDateTime start, LocalDateTime end) {
        return new WorkScheduleDTO(start,end);

    }
}
