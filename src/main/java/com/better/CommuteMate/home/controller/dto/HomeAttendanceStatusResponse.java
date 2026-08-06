package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class HomeAttendanceStatusResponse extends ResponseDetail {
    private AttendanceStatus status;
    private String message;
    private String currentScheduleId;
    private LocalTime scheduleStartTime;
    private LocalTime scheduleEndTime;

    @Builder
    public HomeAttendanceStatusResponse(AttendanceStatus status, String message,
                                        String currentScheduleId,
                                        LocalTime scheduleStartTime,
                                        LocalTime scheduleEndTime) {
        super();
        this.status = status;
        this.message = message;
        this.currentScheduleId = currentScheduleId;
        this.scheduleStartTime = scheduleStartTime;
        this.scheduleEndTime = scheduleEndTime;
    }

    public enum AttendanceStatus {
        NO_SCHEDULE,
        BEFORE_WORK,
        CAN_CHECK_IN,
        WORKING,
        CAN_CHECK_OUT,
        COMPLETED,
        LATE_CHECK_IN
    }
}
