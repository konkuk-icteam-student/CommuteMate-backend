package com.better.CommuteMate.schedule.application.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;

import java.time.LocalDateTime;

public record WorkScheduleCommand(
        Integer userID,
        LocalDateTime start,
        LocalDateTime end) {

    public static WorkSchedule toEntity(WorkScheduleCommand command, User user, CodeType codeType) {
        return WorkSchedule.builder()
                .user(user)
                .startTime(command.start())
                .endTime(command.end())
                .statusCode(codeType)
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .build();
    }

}
