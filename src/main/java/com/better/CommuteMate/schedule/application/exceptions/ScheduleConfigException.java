package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;

public class ScheduleConfigException extends BasicException {

    protected ScheduleConfigException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode, logMessage);
    }

    protected ScheduleConfigException(CustomErrorCode errorCode, String logMessage, ErrorResponseDetail errorResponseDetail) {
        super(errorCode, logMessage, errorResponseDetail);
    }

    public static ScheduleConfigException of(CustomErrorCode errorCode) {
        return new ScheduleConfigException(errorCode, errorCode.getLogMessage());
    }

    public static ScheduleConfigException of(CustomErrorCode errorCode, ErrorResponseDetail errorResponseDetail) {
        return new ScheduleConfigException(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }
}
