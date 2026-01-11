package com.better.CommuteMate.attendance.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AttendanceHistoryListResponse extends ResponseDetail {
    private List<AttendanceHistoryResponse> histories;

    public AttendanceHistoryListResponse(List<AttendanceHistoryResponse> histories) {
        super();
        this.histories = histories;
    }

    public static AttendanceHistoryListResponse of(List<AttendanceHistoryResponse> histories) {
        return new AttendanceHistoryListResponse(histories);
    }
}
