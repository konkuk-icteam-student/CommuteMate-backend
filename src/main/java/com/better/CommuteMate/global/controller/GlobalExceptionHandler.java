package com.better.CommuteMate.global.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.MonthlyWorkTimeExceededException;
import com.better.CommuteMate.schedule.controller.schedule.dtos.MonthlyLimitExceededResponseDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    protected ResponseEntity<Response> handleInvalidRequest(final Exception e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
                Response.error("INVALID_REQUEST", "요청 값이 올바르지 않습니다.")
        );
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<Response> handleCustomException(final CustomException e) {
        //TODO: log 저장 규칙 지정
        log.error("{}: {}", e.getHttpStatus(), e.getLogMessage(), e);
        Response response = Response.error(e.getErrorCode().getName(), e.getMessage());
        return new ResponseEntity<>(response, e.getHttpStatus());
    }

    /**
     * @Valid 유효성 검사 실패 시 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Response> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        // 첫 번째 에러 메시지만 반환
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("Validation failed: {}", errorMessage);
        Response response = Response.error("INVALID_REQUEST", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 권한 없음 예외 처리 (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Response> handleAccessDeniedException(final AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        Response response = Response.error("ACCESS_DENIED", "해당 작업을 수행할 권한이 없습니다.");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Response> handleException(final Exception e) {

        log.error("Unhandled exception", e);

        Response response = Response.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MonthlyWorkTimeExceededException.class)
    public ResponseEntity<Response> handleMonthlyWorkTimeExceeded(
            MonthlyWorkTimeExceededException e
    ) {
        return ResponseEntity.unprocessableEntity().body(Response.of(
                false,
                "월 최대 근무 시간을 초과하였습니다.",
                MonthlyLimitExceededResponseDetail.of(
                        e.getLimitHours(),
                        e.getRequestedHours()
                )
        ));
    }
}
