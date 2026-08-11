package com.better.CommuteMate.admin.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class AdminWorkerDetailResponse extends ResponseDetail {
    @JsonFormat(pattern = "yyyy-MM-dd") public final LocalDate date;
    public final Long userId;
    public final String name;
    public final String studentId;
    public final String department;
    public final Integer grade;
    public final String phoneNumber;
    public final String email;
    @JsonFormat(pattern = "yyyy-MM-dd") public final LocalDate workStartDate;
    public final int weeklyWorkedMinutes;
    public final int weeklyLimitMinutes;
    public final int monthlyWorkedMinutes;
    public final int monthlyLimitMinutes;
    public final long totalChangeRequestCount;
    public final long approvedChangeRequestCount;

    public AdminWorkerDetailResponse(LocalDate date, Long userId, String name, String studentId,
                                     String department, Integer grade, String phoneNumber, String email,
                                     LocalDate workStartDate, int weeklyWorkedMinutes, int weeklyLimitMinutes,
                                     int monthlyWorkedMinutes, int monthlyLimitMinutes,
                                     long totalChangeRequestCount, long approvedChangeRequestCount) {
        this.date = date;
        this.userId = userId;
        this.name = name;
        this.studentId = studentId;
        this.department = department;
        this.grade = grade;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.workStartDate = workStartDate;
        this.weeklyWorkedMinutes = weeklyWorkedMinutes;
        this.weeklyLimitMinutes = weeklyLimitMinutes;
        this.monthlyWorkedMinutes = monthlyWorkedMinutes;
        this.monthlyLimitMinutes = monthlyLimitMinutes;
        this.totalChangeRequestCount = totalChangeRequestCount;
        this.approvedChangeRequestCount = approvedChangeRequestCount;
    }
}
