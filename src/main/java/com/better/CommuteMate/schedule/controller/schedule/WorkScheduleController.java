package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkScheduleResponseDetail;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 근무 일정", description = "사용자 근무 일정 신청 및 수정 API")
@RestController
@RequestMapping("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "근무 일정 신청", description = "여러 근무 시간대를 일괄 신청합니다.")
    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(
            @RequestBody ApplyWorkSchedule request,
            @RequestHeader Integer userId) {
        // @RequestHeader int userID는 추후 인증로직이 추가되면 제거할 예정
        ApplyScheduleResultCommand applyResult = scheduleService.applyWorkSchedules(
                request.slots().stream().map(slot -> new WorkScheduleCommand(
                        userId, slot.start(), slot.end()
                )).toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 등록되었습니다.",
                ApplyWorkScheduleResponseDetail.from(applyResult)
        ));
    }
    @Operation(summary = "근무 일정 수정", description = "기존 근무 일정을 취소하면서 새로운 일정을 추가 신청합니다. 취소는 schedule ID로, 추가는 시간대로 요청합니다.")
    @PatchMapping("/modify")
    public ResponseEntity<Response> modifyWorkSchedule(
            @RequestBody ModifyWorkScheduleDTO request,
            @RequestHeader Integer userId) {
        // @RequestHeader int userID는 추후 인증로직이 추가되면 제거할 예정
         scheduleService.modifyWorkSchedules(request, userId);
         return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 수정(요청)되었습니다.",
                null
         ));
    }

}
