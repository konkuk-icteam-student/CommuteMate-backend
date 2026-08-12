package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public class AdminUserAttendancePageResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate date;
    public final List<UserAttendance> users;
    public final int page;
    public final int size;
    public final long totalElements;
    public final int totalPages;

    public AdminUserAttendancePageResponse(
            LocalDate date,
            List<UserAttendance> users,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        this.date = date;
        this.users = users;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public record UserAttendance(
            String userId,
            String userName,
            String department,
            String studentId,
            String workStatusCode,
            String attendanceStatusCode,
            int lateCount,
            int lateMinutes,
            int weeklyWorkedMinutes,
            int weeklyLimitMinutes,
            int monthlyWorkedMinutes,
            int monthlyLimitMinutes
    ) {
    }
}
