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
        log.error("{}: {}", e.getHttpStatus(), e.getMessage(), e);
        Response response = new Response(false, e.getMessage(), e.getErrorResponseDetail());
        return new ResponseEntity<>(response, e.getHttpStatus());
    }

    @ExceptionHandler(ScheduleAllFailureException.class)
    public ResponseEntity<Response> handleScheduleAllFailureException(final ScheduleAllFailureException e) {
        log.error("Failed to schedule all items - complete scheduling failure: {}", e.getMessage(), e);
        final Response response = new Response(false, e.getMessage(), e.getErrorResponseDetail());
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(SchedulePartialFailureException.class)
    public ResponseEntity<Response> handleSchedulePartialFailureException(final SchedulePartialFailureException e) {
        log.error("Partial scheduling failure - some items could not be scheduled: {}", e.getMessage(), e);
        final Response response = new Response(false, e.getMessage(), e.getErrorResponseDetail());
        return new ResponseEntity<>(response, HttpStatus.MULTI_STATUS);
    }
}
