package com.better.CommuteMate.controller.schedule.dtos;

import com.better.CommuteMate.application.schedule.dtos.WorkScheduleCommand;

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
