package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 월 최대 근무 시간 초과 응답 상세 DTO
 */
@Getter
@Builder
public class MonthlyLimitExceededResponseDetail extends ResponseDetail {

    private final int limitHours;
    private final int requestedHours;

    public static MonthlyLimitExceededResponseDetail of(int limitHours, int requestedHours) {
        return MonthlyLimitExceededResponseDetail.builder()
                .limitHours(limitHours)
                .requestedHours(requestedHours)
                .build();
    }
}