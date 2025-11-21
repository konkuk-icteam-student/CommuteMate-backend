package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements CustomErrorCode {
    NOT_FOUND("찾을 수 없습니다. 다시 확인해주세요", "[Error] : 404 에러 발생", HttpStatus.MULTI_STATUS),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다. 다시 확인해주세요", "[Error] : 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    GlobalErrorCode(String message, String logMessage, HttpStatus status) {
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
