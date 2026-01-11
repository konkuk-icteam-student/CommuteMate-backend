package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserWorkTimeResponse extends ResponseDetail {
    private final long totalMinutes;
    private final String periodType;

    @Builder
    public UserWorkTimeResponse(long totalMinutes, String periodType) {
        this.totalMinutes = totalMinutes;
        this.periodType = periodType;
    }
}
