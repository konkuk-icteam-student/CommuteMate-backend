package com.better.CommuteMate.application.schedule.exceptions.response;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;

import java.util.List;

@Builder
public class ScheduleResponseDetail extends ErrorResponseDetail {
    List<WorkSchedule> success;
    List<WorkSchedule> failure;

    public  static ScheduleResponseDetail of(List<WorkSchedule> success, List<WorkSchedule> failure){
        return ScheduleResponseDetail.builder()
                .success(success)
                .failure(failure)
                .build();
    }
}
