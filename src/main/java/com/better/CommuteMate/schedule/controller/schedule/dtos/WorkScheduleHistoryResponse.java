package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WorkScheduleHistoryResponse extends ResponseDetail {
    private final Long id;
    private final String userName;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final CodeType status;
    
    private final LocalDateTime actualStart;
    private final LocalDateTime actualEnd;
    private final Long workDurationMinutes;

    @Builder
    public WorkScheduleHistoryResponse(Long id, String userName, LocalDateTime start, LocalDateTime end, CodeType status,
                                       LocalDateTime actualStart, LocalDateTime actualEnd, Long workDurationMinutes) {
        this.id = id;
        this.userName = userName;
        this.start = start;
        this.end = end;
        this.status = status;
        this.actualStart = actualStart;
        this.actualEnd = actualEnd;
        this.workDurationMinutes = workDurationMinutes;
    }
}
