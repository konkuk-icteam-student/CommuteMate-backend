package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements CustomErrorCode {
    SCHEDULE_PARTIAL_FAILURE("신청하신 일정 중 실패한 일정이 존재합니다.", "[Error] : 신청하신 일정 중 실패한 일정이 존재합니다.", HttpStatus.MULTI_STATUS),
    SCHEDULE_FAILURE("신청하신 일정이 모두 실패하였습니다.", "[Error] : 신청하신 일정이 모두 실패하였습니다.", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    ScheduleErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
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

    @Override
    public HttpStatus getStatus() {
        return null;
    }
}
