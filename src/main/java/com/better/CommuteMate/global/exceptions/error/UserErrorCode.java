package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum UserErrorCode implements CustomErrorCode {
    ADMIN_USER_SEARCH_KEYWORD_REQUIRED(
            "검색어를 입력해주세요.",
            "[Error] : admin user search keyword required",
            HttpStatus.BAD_REQUEST
    );

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    UserErrorCode(String message, String logMessage, HttpStatus status) {
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
