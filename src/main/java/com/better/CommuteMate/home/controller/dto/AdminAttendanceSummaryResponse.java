package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class AdminAttendanceSummaryResponse extends ResponseDetail {

    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate date;
    public final int currentWorkingCount;
    public final int notCheckedInCount;
    public final int lateCount;
    public final TodayTask todayTask;

    public AdminAttendanceSummaryResponse(
            LocalDate date,
            int currentWorkingCount,
            int notCheckedInCount,
            int lateCount,
            TodayTask todayTask
    ) {
        this.date = date;
        this.currentWorkingCount = currentWorkingCount;
        this.notCheckedInCount = notCheckedInCount;
        this.lateCount = lateCount;
        this.todayTask = todayTask;
    }

    public record TodayTask(
            int completedCount,
            int totalCount
    ) {
    }
}
