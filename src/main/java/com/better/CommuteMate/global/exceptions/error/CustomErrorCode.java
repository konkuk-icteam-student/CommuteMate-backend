package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public interface CustomErrorCode {
    String getLogMessage();

    String getName();

    String getMessage();

    HttpStatus getStatus();
}
