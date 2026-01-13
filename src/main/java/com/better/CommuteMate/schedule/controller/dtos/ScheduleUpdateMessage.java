package com.better.CommuteMate.schedule.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateMessage {
    private String type; // "SCHEDULE_UPDATED"
    private List<SlotUpdateInfo> updates;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotUpdateInfo {
        private boolean isAdd;
        private LocalDateTime slotStartTime;
    }
}
