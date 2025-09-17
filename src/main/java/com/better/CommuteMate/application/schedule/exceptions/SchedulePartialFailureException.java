package com.better.CommuteMate.application.schedule.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class SchedulePartialFailureException extends BasicException {

    protected SchedulePartialFailureException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode, logMessage);
    }

    protected SchedulePartialFailureException(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }
    public static SchedulePartialFailureException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new SchedulePartialFailureException(errorCode,errorResponseDetail);
    }

}
