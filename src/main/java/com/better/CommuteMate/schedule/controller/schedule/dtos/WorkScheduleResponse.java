package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class WorkScheduleResponse extends ResponseDetail {
    private final Long id;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final CodeType status;

    public WorkScheduleResponse(Long id, LocalDateTime start, LocalDateTime end, CodeType status) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public static WorkScheduleResponse from(WorkSchedule schedule) {
        return new WorkScheduleResponse(
            schedule.getScheduleId(),
            schedule.getStartTime(),
            schedule.getEndTime(),
            schedule.getStatusCode()
        );
    }
}