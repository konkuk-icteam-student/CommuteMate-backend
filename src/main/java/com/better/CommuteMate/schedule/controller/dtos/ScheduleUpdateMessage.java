package com.better.CommuteMate.schedule.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateMessage {
    private String type; // "SCHEDULE_UPDATED"
    private LocalDate targetDate; // 변경된 스케줄의 날짜 (클라이언트가 해당 날짜를 새로고침 하도록)
    private String message;
}
