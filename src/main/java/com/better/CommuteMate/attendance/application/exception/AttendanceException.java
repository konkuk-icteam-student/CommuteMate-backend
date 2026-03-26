package com.better.CommuteMate.attendance.application.exception;

import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class AttendanceException extends CustomException {
    public AttendanceException(CustomErrorCode errorCode) {
        super(errorCode, errorCode.getLogMessage());
    }
}
