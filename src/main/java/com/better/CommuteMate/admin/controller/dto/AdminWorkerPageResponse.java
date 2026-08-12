package com.better.CommuteMate.admin.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public class AdminWorkerPageResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate date;
    public final List<Worker> workers;
    public final int page;
    public final int size;
    public final long totalElements;
    public final int totalPages;
    public final boolean first;
    public final boolean last;

    public AdminWorkerPageResponse(LocalDate date, List<Worker> workers, int page, int size,
                                   long totalElements, int totalPages, boolean first, boolean last) {
        this.date = date;
        this.workers = workers;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public record Worker(
            Long userId,
            String name,
            String studentId,
            String department,
            Integer grade,
            String phoneNumber,
            int weeklyWorkedMinutes,
            int weeklyLimitMinutes,
            int monthlyWorkedMinutes,
            int monthlyLimitMinutes,
            long totalChangeRequestCount,
            long approvedChangeRequestCount,
            long totalAttendanceIssueCount,
            long lateCount
    ) {}
}
