package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum AdminWorkerErrorCode implements CustomErrorCode {
    INVALID_REQUEST("잘못된 조회 조건입니다.", "[Error] : invalid admin worker query", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_SETTING_NOT_FOUND("해당 월의 근무 설정을 찾을 수 없습니다.", "[Error] : work schedule setting not found", HttpStatus.NOT_FOUND),
    WORKER_NOT_FOUND("근무 인원을 찾을 수 없습니다.", "[Error] : admin worker not found", HttpStatus.NOT_FOUND),
    WORKER_ACCESS_DENIED("근무 인원 조회 권한이 없습니다.", "[Error] : admin worker access denied", HttpStatus.FORBIDDEN),
    INVALID_WORKER_UPDATE("근무 인원 정보 입력값이 올바르지 않습니다.", "[Error] : invalid admin worker update", HttpStatus.BAD_REQUEST),
    WORKER_UPDATE_ACCESS_DENIED("근무 인원 정보를 수정할 권한이 없습니다.", "[Error] : admin worker update access denied", HttpStatus.FORBIDDEN);

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
