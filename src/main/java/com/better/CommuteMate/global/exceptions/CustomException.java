package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final CustomErrorCode errorCode;

    public CustomException(CustomErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static CustomException of(CustomErrorCode errorCode) {
        return new CustomException(errorCode);
    }

    public String getLogMessage() {
        return errorCode.getLogMessage();
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getStatus();
    }

}