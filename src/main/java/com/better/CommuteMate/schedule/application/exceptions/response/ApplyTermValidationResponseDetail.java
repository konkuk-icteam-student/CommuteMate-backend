package com.better.CommuteMate.schedule.application.exceptions.response;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplyTermValidationResponseDetail extends ErrorResponseDetail {
    private final String errorReason;
    private final String receivedApplyStartTime;
    private final String receivedApplyEndTime;

    public static ApplyTermValidationResponseDetail of(String applyStartTime, String applyEndTime) {
        return ApplyTermValidationResponseDetail.builder()
                .errorReason("신청 시작 시간이 종료 시간보다 늦거나 같습니다.")
                .receivedApplyStartTime(applyStartTime)
                .receivedApplyEndTime(applyEndTime)
                .build();
    }
}
