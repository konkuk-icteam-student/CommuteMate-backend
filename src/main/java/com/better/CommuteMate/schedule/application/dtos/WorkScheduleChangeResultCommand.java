package com.better.CommuteMate.schedule.application.dtos;

import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeResponseDetail;

import java.util.List;

public record WorkScheduleChangeResultCommand(
        List<WorkScheduleChangeResponseDetail.Slot> success,
        List<WorkScheduleChangeResponseDetail.Slot> failure
) {
    public static WorkScheduleChangeResultCommand of(
            List<WorkScheduleChangeResponseDetail.Slot> success,
            List<WorkScheduleChangeResponseDetail.Slot> failure
    ) {
        return new WorkScheduleChangeResultCommand(success, failure);
    }

    public boolean isAllSuccess() {
        return !success.isEmpty() && failure.isEmpty();
    }

    public boolean isPartialSuccess() {
        return !success.isEmpty() && !failure.isEmpty();
    }

    public boolean isAllFailure() {
        return success.isEmpty() && !failure.isEmpty();
    }
}