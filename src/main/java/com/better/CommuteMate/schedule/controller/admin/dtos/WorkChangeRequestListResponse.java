package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class WorkChangeRequestListResponse extends ResponseDetail {
    public final int year;
    public final int month;
    public final String statusCode;
    public final Summary summary;
    public final List<RequestItem> requests;
    public final int page;
    public final int size;
    public final long totalElements;
    public final int totalPages;

    public WorkChangeRequestListResponse(
            int year,
            int month,
            String statusCode,
            Summary summary,
            List<RequestItem> requests,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        this.year = year;
        this.month = month;
        this.statusCode = statusCode;
        this.summary = summary;
        this.requests = requests;
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
            long totalCount,
            long pendingCount,
            long approvedCount,
            long rejectedCount
    ) {
    }

    public record RequestItem(
            String requestId,
            String userId,
            String userName,
            String statusCode,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime processedAt,
            String reason,
            String rejectReason,
            List<ScheduleItem> deleteSchedules,
            List<ScheduleItem> addSchedules
    ) {
    }

    public record ScheduleItem(
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end,
            String changeTypeCode
    ) {
    }
}
