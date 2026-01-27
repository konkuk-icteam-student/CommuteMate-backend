package com.better.CommuteMate.schedule.controller.schedule.dtos;

import java.util.List;

public record ModifyWorkScheduleDTO(
        List<WorkScheduleDTO> applySlots,
        List<Long> cancelScheduleIds,
        String reason
) {
}
