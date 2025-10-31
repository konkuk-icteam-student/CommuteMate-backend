package com.better.CommuteMate.application.schedule.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;

import java.time.LocalDateTime;

public record WorkScheduleCommand(
        String email,
        LocalDateTime start,
        LocalDateTime end) {

    public static WorkSchedule toEntity(WorkScheduleCommand command, User user) {
        return WorkSchedule.builder()
                .user(user)
                .startTime(command.start())
                .endTime(command.end())
                .statusCode(CodeType.WS02)
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .build();
    }

}
