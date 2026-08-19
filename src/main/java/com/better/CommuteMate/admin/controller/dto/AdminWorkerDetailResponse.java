package com.better.CommuteMate.admin.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Schema(description = "조회 기준일(date)이 속한 월에 신청한 총 근무시간(분). 해당 월 WS02 승인 슬롯의 합. 예: 1620(=27시간)", example = "1620")
    public final long submittedMinutes;
    @Schema(description = "최근 근무 신청 시각. 수정요청 제외, apply 신청 중 가장 마지막. 신청 이력 없으면 null", example = "2026-07-15 13:00")
    public final LocalDateTime lastRequestedAt;

    public AdminWorkerDetailResponse(LocalDate date, Long userId, String name, String studentId,
                                     String department, Integer grade, String phoneNumber, String email,
                                     LocalDate workStartDate, int weeklyWorkedMinutes, int weeklyLimitMinutes,
                                     int monthlyWorkedMinutes, int monthlyLimitMinutes,
                                     long totalChangeRequestCount, long approvedChangeRequestCount,
                                     long submittedMinutes, LocalDateTime lastRequestedAt) {
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
        this.submittedMinutes = submittedMinutes;
        this.lastRequestedAt = lastRequestedAt;
    }
}
