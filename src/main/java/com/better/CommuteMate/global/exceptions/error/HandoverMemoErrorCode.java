package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum HandoverMemoErrorCode implements CustomErrorCode {
    INVALID_DATE_FORMAT(
            "날짜 형식이 올바르지 않습니다.",
            "[Error] : invalid handover memo date format",
            HttpStatus.BAD_REQUEST
    );

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    HandoverMemoErrorCode(String message, String logMessage, HttpStatus status) {
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
