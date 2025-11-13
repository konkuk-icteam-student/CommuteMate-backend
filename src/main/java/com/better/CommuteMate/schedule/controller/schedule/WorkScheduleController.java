package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkScheduleResponseDetail;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(@RequestBody ApplyWorkSchedule request, @RequestHeader Integer userId) {
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

}
