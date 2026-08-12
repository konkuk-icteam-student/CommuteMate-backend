package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum HandoverMemoErrorCode implements CustomErrorCode {
    INVALID_DATE_FORMAT(
            "날짜 형식이 올바르지 않습니다.",
            "[Error] : invalid handover memo date format",
            HttpStatus.BAD_REQUEST
    ),
    MEMO_NOT_FOUND(
            "인수인계 메모를 찾을 수 없습니다.",
            "[Error] : handover memo not found",
            HttpStatus.NOT_FOUND
    ),
    MEMO_DELETE_FORBIDDEN(
            "본인이 작성한 메모만 삭제할 수 있습니다.",
            "[Error] : not the author of the handover memo",
            HttpStatus.FORBIDDEN
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
