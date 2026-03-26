package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final String logMessage;
    private final HttpStatus httpStatus;

    public CustomException(CustomErrorCode errorCode) {
        super(errorCode.getMessage());
        this.logMessage = errorCode.getLogMessage();
        this.httpStatus = errorCode.getStatus();
    }

    public static CustomException of(CustomErrorCode errorCode) {
        return new CustomException(errorCode);
    }
}