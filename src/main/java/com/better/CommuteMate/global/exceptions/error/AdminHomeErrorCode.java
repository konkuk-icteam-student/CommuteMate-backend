package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum AdminHomeErrorCode implements CustomErrorCode {
    INVALID_DATE(
            "조회 날짜 값이 올바르지 않습니다.",
            "[Error] : invalid admin home attendance summary date",
            HttpStatus.BAD_REQUEST
    );

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    AdminHomeErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
    }

    @Override
    public String getLogMessage() {
        return logMessage;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}
