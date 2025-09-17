package com.better.CommuteMate.application.schedule.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public enum ScheduleErrorCode implements CustomErrorCode {
    SCHEDULE_PARTIAL_FAILURE("신청하신 일정 중 실패한 일정이 존재합니다.", "[Error] : 신청하신 일정 중 실패한 일정이 존재합니다."),
    SCHEDULE_FAILURE("신청하신 일정이 모두 실패하였습니다.", "[Error] : 신청하신 일정이 모두 실패하였습니다.");

    private final String message;
    private final String logMessage;

    ScheduleErrorCode(String message, String logMessage) {
        this.message = message;
        this.logMessage = logMessage;
    }

    @Override
    public String getLogMessage() {
        return this.logMessage;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
