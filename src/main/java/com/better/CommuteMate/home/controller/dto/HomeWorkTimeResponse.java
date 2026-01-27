package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HomeWorkTimeResponse extends ResponseDetail {
    private long totalMinutes;
    private int scheduleCount;

    @Builder
    public HomeWorkTimeResponse(long totalMinutes, int scheduleCount) {
        super();
        this.totalMinutes = totalMinutes;
        this.scheduleCount = scheduleCount;
    }
}
