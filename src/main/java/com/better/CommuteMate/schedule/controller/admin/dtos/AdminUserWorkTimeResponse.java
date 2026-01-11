package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AdminUserWorkTimeResponse extends ResponseDetail {
    private final UserInfoResponse userInfo;
    private final long totalMinutes;

    @Builder
    public AdminUserWorkTimeResponse(UserInfoResponse userInfo, long totalMinutes) {
        this.userInfo = userInfo;
        this.totalMinutes = totalMinutes;
    }
}
