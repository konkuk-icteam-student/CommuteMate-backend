package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ApplyWorkScheduleResponseDetail extends ResponseDetail {
    private final List<WorkScheduleDTO> success;
    private final List<WorkScheduleDTO> fail;

    public static ApplyWorkScheduleResponseDetail applyWorkScheduleResponseDetail(List<WorkScheduleDTO> success, List<WorkScheduleDTO> fail) {
        return ApplyWorkScheduleResponseDetail.builder()
                .success(success)
                .fail(fail)
                .build();
    }
    public static ApplyWorkScheduleResponseDetail from(ApplyScheduleResultCommand applyScheduleResultCommand) {
        return applyWorkScheduleResponseDetail(applyScheduleResultCommand.success(), applyScheduleResultCommand.fail());
    }
}
