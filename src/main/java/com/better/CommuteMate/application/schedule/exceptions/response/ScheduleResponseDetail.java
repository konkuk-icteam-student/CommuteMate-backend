package com.better.CommuteMate.application.schedule.exceptions.response;

import com.better.CommuteMate.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.application.schedule.dtos.ApplyScheduleResultCommand;
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
