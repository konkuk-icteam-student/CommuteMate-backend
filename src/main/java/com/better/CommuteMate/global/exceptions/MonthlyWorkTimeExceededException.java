package com.better.CommuteMate.global.exceptions;

import lombok.Getter;

/**
 * 월 최대 근무 시간 초과 예외
 * limitHours, requestedHours를 응답 details에 포함하기 위해 사용
 */
@Getter
public class MonthlyWorkTimeExceededException extends RuntimeException {

    private final int limitHours;
    private final int requestedHours;

    public MonthlyWorkTimeExceededException(int limitHours, int requestedHours) {
        super("월 최대 근무 시간을 초과하였습니다.");
        this.limitHours = limitHours;
        this.requestedHours = requestedHours;
    }
}