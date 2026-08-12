package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum TodoErrorCode implements CustomErrorCode {

    INVALID_TODO_DATE(
            "날짜 형식이 올바르지 않습니다.",
            "[Error] : invalid admin todo date format",
            HttpStatus.BAD_REQUEST),
    INVALID_TODO_INFORMATION(
            "업무사항 입력값이 올바르지 않습니다.",
            "[Error] : invalid admin todo information",
            HttpStatus.BAD_REQUEST),
    TODO_UPDATE_ACCESS_DENIED(
            "업무사항을 수정할 권한이 없습니다.",
            "[Error] : admin todo update access denied",
            HttpStatus.FORBIDDEN),
    TODO_DELETE_ACCESS_DENIED(
            "업무사항을 삭제할 권한이 없습니다.",
            "[Error] : admin todo delete access denied",
            HttpStatus.FORBIDDEN),
    TODO_NOT_FOUND(
            "업무사항을 찾을 수 없습니다.",
            "[Error] : admin todo not found",
            HttpStatus.NOT_FOUND);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    TodoErrorCode(String message, String logMessage, HttpStatus status) {
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
