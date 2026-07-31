package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SaveScheduleSettingResponse extends ResponseDetail {
    public final int year;
    public final int month;
    public final LocalDate applyStartDate;
    public final LocalDate applyEndDate;
    public final int maxConcurrentWorkers;
    public final int minWorkUnitMinutes;
    public final int weeklyMinMinutes;
    public final int weeklyMaxMinutes;
    public final int monthlyMinMinutes;
    public final int monthlyMaxMinutes;
    public final List<LocalDate> unavailableDates;
    public final List<SaveScheduleSettingRequest.UnavailableTimeRange> unavailableTimeRanges;
    public final int affectedScheduleCount;
    public final int affectedUserCount;

    public SaveScheduleSettingResponse(
            int year,
            int month,
            SaveScheduleSettingRequest request,
            int affectedScheduleCount,
            int affectedUserCount
    ) {
        this.year = year;
        this.month = month;
        this.applyStartDate = request.applyStartDate();
        this.applyEndDate = request.applyEndDate();
        this.maxConcurrentWorkers = request.maxConcurrentWorkers();
        this.minWorkUnitMinutes = request.minWorkUnitMinutes();
        this.weeklyMinMinutes = request.weeklyMinMinutes();
        this.weeklyMaxMinutes = request.weeklyMaxMinutes();
        this.monthlyMinMinutes = request.monthlyMinMinutes();
        this.monthlyMaxMinutes = request.monthlyMaxMinutes();
        this.unavailableDates = request.unavailableDatesOrEmpty();
        this.unavailableTimeRanges = request.unavailableTimeRangesOrEmpty();
        this.affectedScheduleCount = affectedScheduleCount;
        this.affectedUserCount = affectedUserCount;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
