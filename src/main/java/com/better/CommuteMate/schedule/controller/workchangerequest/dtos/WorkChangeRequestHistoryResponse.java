package com.better.CommuteMate.schedule.controller.workchangerequest.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class WorkChangeRequestHistoryResponse extends ResponseDetail {

    @Schema(description = "조회 연도 (연월 필터가 없으면 null)", example = "2026")
    public final Integer year;

    @Schema(description = "조회 월 (연월 필터가 없으면 null)", example = "4")
    public final Integer month;

    @Schema(description = "신청 상태 필터 (ALL / CS01 / CS02 / CS03)", example = "ALL")
    public final String statusCode;

    @Schema(description = "조회 기간 전체 신청 현황 요약 (상태 필터 무관)")
    public final Summary summary;

    @Schema(description = "신청기록 목록 (신청 시각 기준 최신순)")
    public final List<HistoryItem> histories;

    @Schema(description = "현재 페이지 번호", example = "0")
    public final int page;

    @Schema(description = "페이지당 항목 수", example = "10")
    public final int size;

    @Schema(description = "전체 항목 수", example = "4")
    public final long totalElements;

    @Schema(description = "전체 페이지 수", example = "1")
    public final int totalPages;

    public WorkChangeRequestHistoryResponse(
            Integer year,
            Integer month,
            String statusCode,
            Summary summary,
            List<HistoryItem> histories,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        this.year = year;
        this.month = month;
        this.statusCode = statusCode;
        this.summary = summary;
        this.histories = histories;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record Summary(
            @Schema(description = "전체 신청 건수", example = "4") long totalCount,
            @Schema(description = "승인 건수", example = "2") long approvedCount,
            @Schema(description = "대기 건수", example = "1") long pendingCount,
            @Schema(description = "거절 건수", example = "1") long rejectedCount
    ) {}

    public record HistoryItem(
            @Schema(description = "신청 ID", example = "1")
            Long requestId,

            @Schema(description = "신청 상태 코드 (CS01: 대기, CS02: 승인, CS03: 거절)", example = "CS01")
            String statusCode,

            @Schema(description = "신청 상태 표시명", example = "대기")
            String statusName,

            @Schema(description = "신청 시각", example = "2026-04-01T10:00:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime requestedAt,

            @Schema(description = "처리 시각 (미처리 시 null)")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime processedAt,

            @Schema(description = "신청 사유", example = "학과 행사 일정으로 인해 근무시간 변경을 요청합니다.")
            String reason,

            @Schema(description = "거절 사유 (CS03 거절 시에만 존재)")
            String rejectReason,

            @Schema(description = "삭제 근무시간 목록 (CR02)")
            List<SlotItem> deleteSlots,

            @Schema(description = "추가 근무시간 목록 (CR01)")
            List<SlotItem> addSlots
    ) {}

    public record SlotItem(
            @Schema(description = "시작 일시", example = "2026-04-06T13:00:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime start,

            @Schema(description = "종료 일시", example = "2026-04-06T14:30:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime end,

            @Schema(description = "변경 유형 코드 (CR01: 추가, CR02: 삭제)", example = "CR02")
            String changeTypeCode
    ) {}
}
