package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;

@Getter
public class BasicException extends RuntimeException {
    private final String logMessage;
    private final ErrorResponseDetail errorResponseDetail;

    protected BasicException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode.getMessage());
        this.logMessage = logMessage;
        this.errorResponseDetail = null;
    }
    protected BasicException(CustomErrorCode errorCode, String logMessage, ErrorResponseDetail errorResponseDetail) {
        super(errorCode.getMessage());
        this.logMessage = logMessage;
        this.errorResponseDetail = errorResponseDetail;
    }

    public static BasicException of(CustomErrorCode errorCode) {
        return new BasicException(errorCode, errorCode.getLogMessage());
    }

    public static BasicException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new BasicException(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

}
