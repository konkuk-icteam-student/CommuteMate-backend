package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements CustomErrorCode {
    SCHEDULE_PARTIAL_FAILURE("신청하신 일정 중 실패한 일정이 존재합니다.", "[Error] : 신청하신 일정 중 실패한 일정이 존재합니다.", HttpStatus.MULTI_STATUS),
    SCHEDULE_FAILURE("신청하신 일정이 모두 실패하였습니다.", "[Error] : 신청하신 일정이 모두 실패하였습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_APPLY_TERM("신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.", "[Error] : 신청 기간 유효성 검증 실패", HttpStatus.BAD_REQUEST),
    MONTHLY_SCHEDULE_CONFIG_NOT_FOUND("해당 연월의 스케줄 설정을 찾을 수 없습니다.", "[Error] : 월별 스케줄 설정 미존재", HttpStatus.NOT_FOUND);


    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    ScheduleErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
    }

    @Override
    public String getLogMessage() {
        return this.logMessage;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }
}
