package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class ScheduleAllFailureException extends BasicException {

    protected ScheduleAllFailureException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode, logMessage);
    }

    protected ScheduleAllFailureException(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }

    public static ScheduleAllFailureException of(CustomErrorCode errorCode) {
        return new ScheduleAllFailureException(errorCode, errorCode.getLogMessage());
    }

    public static ScheduleAllFailureException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new ScheduleAllFailureException(errorCode, errorResponseDetail);
    }
}
