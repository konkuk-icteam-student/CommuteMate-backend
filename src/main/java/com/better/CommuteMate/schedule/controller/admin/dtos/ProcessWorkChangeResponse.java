package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessWorkChangeResponse extends ResponseDetail {
    public final Long requestId;
    public final String statusCode;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public final LocalDateTime processedAt;
    public final String rejectReason;
    public final List<ScheduleResult> deleteSchedules;
    public final List<ScheduleResult> addSchedules;

    public ProcessWorkChangeResponse(
            Long requestId,
            String statusCode,
            LocalDateTime processedAt,
            String rejectReason,
            List<ScheduleResult> deleteSchedules,
            List<ScheduleResult> addSchedules
    ) {
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.processedAt = processedAt;
        this.rejectReason = rejectReason;
        this.deleteSchedules = deleteSchedules;
        this.addSchedules = addSchedules;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record ScheduleResult(
            String scheduleId,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end,
            String statusCode
    ) {
    }
}
