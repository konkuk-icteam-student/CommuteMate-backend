package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class HomeAttendanceStatusResponse extends ResponseDetail {
    private AttendanceStatus status;
    private String message;
    private Long currentScheduleId;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;

    @Builder
    public HomeAttendanceStatusResponse(AttendanceStatus status, String message, 
                                        Long currentScheduleId,
                                        LocalDateTime scheduleStartTime, 
                                        LocalDateTime scheduleEndTime) {
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
