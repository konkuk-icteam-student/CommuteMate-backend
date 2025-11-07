package com.better.CommuteMate.auth.controller;

import com.better.CommuteMate.auth.application.AuthErrorCode;
import com.better.CommuteMate.global.controller.dtos.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.better.CommuteMate.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.error("Auth - Invalid argument: {}", e.getMessage());
        final Response response = new Response(false, e.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Response> handleIllegalStateException(final IllegalStateException e) {
        log.error("Auth - Illegal state: {}", e.getMessage());
        final Response response = new Response(false, e.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleGeneralException(final Exception e) {
        log.error("Auth - Unexpected error: {}", e.getMessage(), e);
        final Response response = new Response(false, AuthErrorCode.INTERNAL_AUTH_ERROR.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
