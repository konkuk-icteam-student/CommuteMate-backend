package com.better.CommuteMate.notification.controller.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            @Schema(description = "알림 ID", example = "1")
            Long notificationId,

            @Schema(description = "알림 유형 코드 (NT01: 근무 변경 요청 승인, NT02: 근무 변경 요청 거절, NT03: 근무 신청 시작)",
                    example = "NT02", allowableValues = {"NT01", "NT02", "NT03"})
            String typeCode,

            @Schema(description = "알림 유형 표시명 (typeCode에 대응하는 한글 라벨)", example = "근무 변경 요청 거절")
            String typeName,

            @Schema(description = "알림 제목", example = "근무 시간 수정이 거절되었습니다.")
            String title,

            @Schema(description = "변경 항목 목록. NT01/NT02는 해당 요청의 변경 항목들, NT03은 항상 빈 배열([])입니다. " +
                    "문자열이 아닌 배열로 내려가므로 프론트에서 별도 JSON.parse 없이 content[i].date 형태로 접근합니다.")
            List<ContentItem> content,

            @Schema(description = "반려 사유. NT02(반려)일 때만 값이 있고, NT01/NT03은 null입니다.",
                    example = "정원 초과로 반려합니다.", nullable = true)
            String rejectReason,

            @Schema(description = "참조 대상 ID (null 가능). NT01/NT02는 work_change_request의 requestId, " +
                    "NT03은 work_schedule_setting의 settingId입니다.", example = "37")
            String refId,

            @Schema(description = "알림 생성 시각", example = "2026-03-20T16:21:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,

            @Schema(description = "새 알림 여부 (마지막 알림함 확인 시각 이후 생성된 경우 true)", example = "true")
            boolean isNew
    ) {}

    public record ContentItem(
            @Schema(description = "변경 대상 날짜", example = "2026-04-06")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate date,

            @Schema(description = "시작 시각", example = "13:00")
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,

            @Schema(description = "종료 시각", example = "13:30")
            @JsonFormat(pattern = "HH:mm")
            LocalTime endTime,

            @Schema(description = "소요 시간(분) — raw 정수로 내려가며, 표시 환산(예: 1.5h)은 프론트 책임입니다.", example = "30")
            int durationMinutes,

            @Schema(description = "변경 유형 코드 (CodeType: major=CR, sub=01/02). "
                    + "CodeType의 codeName/codeValue는 EDIT/'수정 요청'(CR01), DELETE/'삭제 요청'(CR02)이지만, "
                    + "work_change_request_item 단위(이 필드가 실려 있는 단위)에서는 "
                    + "CR01=해당 슬롯 추가, CR02=해당 슬롯 삭제를 의미합니다. "
                    + "WorkChangeRequestHistoryResponse.changeTypeCode와 동일한 규약입니다.",
                    example = "CR01", allowableValues = {"CR01", "CR02"})
            String changeTypeCode
    ) {}
}
