package com.better.CommuteMate.application.schedule.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.user.entity.User;

import java.time.LocalDateTime;

public record WorkScheduleCommand(
        String email,
        LocalDateTime start,
        LocalDateTime end) {

    public static WorkSchedule toEntity(WorkScheduleCommand command, User user) {
        return WorkSchedule.builder()
                .user(user)
                .date(command.start.toLocalDate())
                .startTime(command.start().toLocalTime())
                .endTime(command.end().toLocalTime())
                .build();
    }

}
