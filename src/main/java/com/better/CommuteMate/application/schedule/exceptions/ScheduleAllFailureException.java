package com.better.CommuteMate.application.schedule.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class ScheduleAllFailureException extends BasicException {

    protected ScheduleAllFailureException(CustomErrorCode errorCode) {
        super(errorCode, errorCode.getLogMessage());
    }

    protected ScheduleAllFailureException(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }

    public static ScheduleAllFailureException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new ScheduleAllFailureException(errorCode, errorResponseDetail);
    }
}
