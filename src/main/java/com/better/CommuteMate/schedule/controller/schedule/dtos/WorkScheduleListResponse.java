package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;
import java.util.List;

@Getter
public class WorkScheduleListResponse extends ResponseDetail {
    private final List<WorkScheduleResponse> schedules;

    public WorkScheduleListResponse(List<WorkScheduleResponse> schedules) {
        this.schedules = schedules;
    }

    public static WorkScheduleListResponse of(List<WorkScheduleResponse> schedules) {
        return new WorkScheduleListResponse(schedules);
    }
}
