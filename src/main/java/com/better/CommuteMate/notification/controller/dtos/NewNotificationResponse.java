package com.better.CommuteMate.notification.controller.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class NewNotificationResponse extends ResponseDetail {

    @Schema(description = "새 알림 존재 여부 (newNotificationCount >= 1이면 true)", example = "true")
    public final boolean hasNewNotification;

    @Schema(description = "새 알림 개수", example = "3")
    public final long newNotificationCount;

    public NewNotificationResponse(long newNotificationCount) {
        this.newNotificationCount = newNotificationCount;
        this.hasNewNotification = newNotificationCount > 0;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
