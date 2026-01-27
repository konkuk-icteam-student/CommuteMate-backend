package com.better.CommuteMate.attendance.application.exception;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements CustomErrorCode {
    INVALID_QR_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 QR 토큰입니다."),
    NOT_WORK_TIME(HttpStatus.BAD_REQUEST, "출퇴근 가능한 시간이 아닙니다."),
    NO_SCHEDULE_FOUND(HttpStatus.NOT_FOUND, "오늘 예정된 근무 일정이 없습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "이미 출근 처리되었습니다."),
    ALREADY_CHECKED_OUT(HttpStatus.CONFLICT, "이미 퇴근 처리되었습니다."),
    CHECK_IN_REQUIRED(HttpStatus.BAD_REQUEST, "먼저 출근 처리가 필요합니다."),
    TOO_EARLY_CHECK_IN(HttpStatus.BAD_REQUEST, "출근 가능 시간이 아닙니다. (근무 시작 10분 전부터 가능)"),
    TOO_LATE_CHECK_OUT(HttpStatus.BAD_REQUEST, "퇴근 가능 시간이 아닙니다."),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getLogMessage() {
        return this.message;
    }

    @Override
    public String getName() {
        return this.name();
    }
}
