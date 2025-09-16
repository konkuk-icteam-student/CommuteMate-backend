package com.better.CommuteMate.global.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.global.exceptions.BasicException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    protected ResponseEntity<Response> handleBasicException(final BasicException e) {
        final Response response = new Response(false, e.getMessage(), e.getErrorResponseDetail());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
