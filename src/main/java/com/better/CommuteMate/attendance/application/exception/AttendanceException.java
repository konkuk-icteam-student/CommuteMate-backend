package com.better.CommuteMate.attendance.application.exception;

import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class AttendanceException extends BasicException {
    public AttendanceException(CustomErrorCode errorCode) {
        super(errorCode, errorCode.getLogMessage());
    }
}
