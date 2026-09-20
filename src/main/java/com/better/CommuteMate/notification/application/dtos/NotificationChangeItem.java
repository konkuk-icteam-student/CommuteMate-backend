package com.better.CommuteMate.notification.application.dtos;

import com.better.CommuteMate.global.code.CodeType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DB에 저장되는 content JSON의 필드 형식은 응답 DTO(NotificationListResponse.ContentItem)의
 * @JsonFormat과 반드시 일치해야 한다 — 하나라도 어긋나면 저장은 되지만 조회 시 파싱이 실패해
 * content가 통째로 빈 배열로 내려간다.
 */
public record NotificationChangeItem(
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        long durationMinutes,
        CodeType changeTypeCode
) {
}
