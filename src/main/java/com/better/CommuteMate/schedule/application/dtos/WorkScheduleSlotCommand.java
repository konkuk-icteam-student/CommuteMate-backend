package com.better.CommuteMate.schedule.application.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 근무 일정 변경 요청의 단일 시간 슬롯 Command
 * date, start, end를 기준으로 추가/삭제 대상 일정을 표현
 */
public record WorkScheduleSlotCommand(
        LocalDate date,
        LocalTime start,
        LocalTime end
) {

    /**
     * 응답 반환 시 사용할 시작 일시 생성
     */
    public LocalDateTime startDateTime() {
        return LocalDateTime.of(date, start);
    }

    /**
     * 응답 반환 시 사용할 종료 일시 생성
     */
    public LocalDateTime endDateTime() {
        return LocalDateTime.of(date, end);
    }
}