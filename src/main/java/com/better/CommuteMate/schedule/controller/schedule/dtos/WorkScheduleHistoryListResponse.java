package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class WorkScheduleHistoryListResponse extends ResponseDetail {
    private final List<WorkScheduleHistoryResponse> histories;

    public WorkScheduleHistoryListResponse(List<WorkScheduleHistoryResponse> histories) {
        this.histories = histories;
    }

    public static WorkScheduleHistoryListResponse of(List<WorkScheduleHistoryResponse> histories) {
        return new WorkScheduleHistoryListResponse(histories);
    }
}
