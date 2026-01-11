package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
/**
 * 사용자 근무 시간 조회 응답 DTO
 * 특정 기간(주간/월간) 동안의 총 근무 시간을 반환합니다.
 */
public class UserWorkTimeResponse extends ResponseDetail {
    
    /** 총 근무 시간 (분 단위) */
    private final long totalMinutes;
    
    /** 조회 기간 타입 (WEEKLY, MONTHLY) */
    private final String periodType;

    @Builder
    public UserWorkTimeResponse(long totalMinutes, String periodType) {
        this.totalMinutes = totalMinutes;
        this.periodType = periodType;
    }
}
