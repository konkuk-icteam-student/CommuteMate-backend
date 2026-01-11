package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import java.time.LocalDateTime;

public record ApplyRequestResponse(
        Integer scheduleId,
        Integer userId,
        String userName,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static ApplyRequestResponse from(WorkSchedule schedule) {
        return new ApplyRequestResponse(
                schedule.getScheduleId(),
                schedule.getUser().getUserId(),
                schedule.getUser().getName(), // Assuming User has getName()
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
