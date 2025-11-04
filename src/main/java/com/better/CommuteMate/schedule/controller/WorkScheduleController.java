package com.better.CommuteMate.schedule.controller;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleLimit;
import com.better.CommuteMate.schedule.application.MonthlyScheduleLimitService;
import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleLimitCommand;
import com.better.CommuteMate.schedule.controller.dtos.*;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;
    private final MonthlyScheduleLimitService monthlyScheduleLimitService;


    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(@RequestBody ApplyWorkSchedule request, @RequestHeader String email) {
        // @RequestHeader String email은 추후 인증로직이 추가되면 제거할 예정
        ApplyScheduleResultCommand applyResult = scheduleService.applyWorkSchedules(
                request.slots().stream().map(slot -> new WorkScheduleCommand(
                        email, slot.start(), slot.end()
                )).toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 신청되었습니다.",
                ApplyWorkScheduleResponseDetail.from(applyResult)
        ));
    }

    // TODO: 반드시 Admin 권한만 수정이 가능하게 해야 함!
    // 월별 스케줄 제한 설정
    @PostMapping("/monthly-limit")
    public ResponseEntity<Response> setMonthlyLimit(
            @RequestBody SetMonthlyLimitRequest request,
            @RequestHeader(value = "userId", defaultValue = "1") Integer userId) {
        // @RequestHeader userId는 추후 인증로직이 추가되면 변경될 예정

        MonthlyScheduleLimit result = monthlyScheduleLimitService.setMonthlyLimit(
                MonthlyScheduleLimitCommand.from(
                        request.scheduleYear(),
                        request.scheduleMonth(),
                        request.maxConcurrent(),
                        userId
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(Response.of(
                true,
                "월별 스케줄 제한이 설정되었습니다.",
                MonthlyLimitResponse.from(result)
        ));
    }

    // 특정 월의 스케줄 제한 조회
    @GetMapping("/monthly-limit/{year}/{month}")
    public ResponseEntity<Response> getMonthlyLimit(
            @PathVariable Integer year,
            @PathVariable Integer month) {

        return monthlyScheduleLimitService.getMonthlyLimit(year, month)
                .map(limit -> ResponseEntity.ok(Response.of(
                        true,
                        "월별 스케줄 제한을 조회했습니다.",
                        MonthlyLimitResponse.from(limit)
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.of(
                        false,
                        "해당 월의 스케줄 제한 설정을 찾을 수 없습니다. 기본값은 5명 입니다.",
                        null
                )));
    }

    // 모든 월별 스케줄 제한 조회
    @GetMapping("/monthly-limits")
    public ResponseEntity<Response> getAllMonthlyLimits() {
        List<MonthlyScheduleLimit> limits = monthlyScheduleLimitService.getAllMonthlyLimits();

        return ResponseEntity.ok(Response.of(
                true,
                "모든 월별 스케줄 제한을 조회했습니다. 기본값은 5명 입니다.",
                MonthlyLimitsResponse.from(limits)
        ));
    }

}
