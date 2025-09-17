package com.better.CommuteMate.controller.schedule;

import com.better.CommuteMate.application.schedule.ScheduleService;
import com.better.CommuteMate.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.controller.schedule.dtos.ApplyWorkScheduleResponseDetail;
import com.better.CommuteMate.domain.schedule.command.ApplyScheduleResultCommand;
import com.better.CommuteMate.domain.schedule.command.ScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(@RequestBody ApplyWorkSchedule request, @RequestBody String email) {
        // @RequestBody String email은 추후 인증로직이 추가되면 제거할 예정
        ApplyScheduleResultCommand applyResult = scheduleService.applyWorkSchedules(
                request.slots().stream().map(slot -> new ScheduleCommand(
                        email, slot.start(), slot.end()
                )).toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 등록되었습니다.",
                ApplyWorkScheduleResponseDetail.from(applyResult)
        ));
    }

}
