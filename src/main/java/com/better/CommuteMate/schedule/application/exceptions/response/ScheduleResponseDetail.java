package com.better.CommuteMate.schedule.application.exceptions.response;

import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleResponseDetail extends ErrorResponseDetail {
    List<WorkScheduleDTO> success;
    List<WorkScheduleDTO> failure;

    public static ScheduleResponseDetail of(ApplyScheduleResultCommand command){
        return ScheduleResponseDetail.builder()
                .success(command.success())
                .failure(command.fail())
                .build();
    }
}
