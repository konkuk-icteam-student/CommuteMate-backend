package com.better.CommuteMate.global.controller;

import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.global.exceptions.BasicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BasicException.class)
    protected ResponseEntity<Response> handleBasicException(final BasicException e) {
        //TODO: log 저장 규칙 지정
        log.error("{}: {}", e.getHttpStatus(), e.getMessage(), e);
        Response response = new Response(false, e.getMessage(), e.getErrorResponseDetail());
        return new ResponseEntity<>(response, e.getHttpStatus());
    }
}
