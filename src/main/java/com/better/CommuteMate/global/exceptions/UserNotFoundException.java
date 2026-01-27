package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class UserNotFoundException extends BasicException{

    protected UserNotFoundException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode, logMessage);
    }

    protected UserNotFoundException(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }
    public static UserNotFoundException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new UserNotFoundException(errorCode, errorResponseDetail);
    }
}
