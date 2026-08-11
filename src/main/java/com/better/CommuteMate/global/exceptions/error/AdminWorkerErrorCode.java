package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum AdminWorkerErrorCode implements CustomErrorCode {
    INVALID_REQUEST("잘못된 조회 조건입니다.", "[Error] : invalid admin worker query", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_SETTING_NOT_FOUND("해당 월의 근무 설정을 찾을 수 없습니다.", "[Error] : work schedule setting not found", HttpStatus.NOT_FOUND);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    AdminWorkerErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
    }

    @Override public String getLogMessage() { return logMessage; }
    @Override public String getName() { return name(); }
    @Override public String getMessage() { return message; }
    @Override public HttpStatus getStatus() { return status; }
}
