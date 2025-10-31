package com.better.CommuteMate.global.exceptions.error;

public enum GlobalErrorCode implements CustomErrorCode {
    NOT_FOUND("찾을 수 없습니다. 다시 확인해주세요", "[Error] : 404 에러 발생"),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다. 다시 확인해주세요", "[Error] : 사용자를 찾을 수 없습니다.");

    private final String message;
    private final String logMessage;

    GlobalErrorCode(String message, String logMessage) {
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
