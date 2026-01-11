package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminWorkTimeSummaryResponse extends ResponseDetail {
    private final List<AdminUserWorkTimeResponse> summary;

    public AdminWorkTimeSummaryResponse(List<AdminUserWorkTimeResponse> summary) {
        this.summary = summary;
    }

    public static AdminWorkTimeSummaryResponse of(List<AdminUserWorkTimeResponse> summary) {
        return new AdminWorkTimeSummaryResponse(summary);
    }
}
