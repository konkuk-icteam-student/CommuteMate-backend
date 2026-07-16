package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkScheduleEditResponse extends ResponseDetail {

    private final Long requestId;
    private final String status;

    @Builder
    public WorkScheduleEditResponse(Long requestId, String status) {
        this.requestId = requestId;
        this.status = status;
    }
}
