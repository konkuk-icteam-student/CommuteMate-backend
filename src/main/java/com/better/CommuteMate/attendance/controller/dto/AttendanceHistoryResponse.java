package com.better.CommuteMate.attendance.controller.dto;

import com.better.CommuteMate.global.code.CodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceHistoryResponse {
    private Integer attendanceId;
    private LocalDateTime checkTime;
    private CodeType checkType;
    private Integer scheduleId;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
}
