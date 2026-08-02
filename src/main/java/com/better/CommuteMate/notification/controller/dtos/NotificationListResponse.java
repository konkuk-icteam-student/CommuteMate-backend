package com.better.CommuteMate.notification.controller.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationListResponse extends ResponseDetail {

    @Schema(description = "알림 목록 (생성 시각 기준 최신순)")
    public final List<NotificationItem> notifications;

    public NotificationListResponse(List<NotificationItem> notifications) {
        this.notifications = notifications;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record NotificationItem(
            @Schema(description = "알림 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            String notificationId,

            @Schema(description = "알림 유형 코드 (NT01: 근무 변경 요청 승인, NT02: 근무 변경 요청 거절, NT03: 근무 신청 시작)", example = "NT02")
            String typeCode,

            @Schema(description = "알림 유형 표시명", example = "근무 변경 요청 거절")
            String typeName,

            @Schema(description = "알림 제목", example = "근무 시간 수정이 거절되었습니다.")
            String title,

            @Schema(description = "알림 내용 (null 가능)", example = "4월 6일 13:00-14:30 (1.5h)")
            String content,

            @Schema(description = "참조 대상 ID (UUID, null 가능)", example = "9a1b2c3d-e29b-41d4-a716-446655440000")
            String refId,

            @Schema(description = "알림 생성 시각", example = "2026-03-20T16:21:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,

            @Schema(description = "새 알림 여부 (마지막 알림함 확인 시각 이후 생성된 경우 true)", example = "true")
            boolean isNew
    ) {}
}
