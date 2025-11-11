package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.schedule.application.MonthlyScheduleLimitService;
import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleLimitCommand;
import com.better.CommuteMate.schedule.controller.admin.dtos.MonthlyLimitResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.MonthlyLimitsResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.SetMonthlyLimitRequest;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.global.controller.dtos.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/admin/schedule")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final MonthlyScheduleLimitService monthlyScheduleLimitService;

    // 월별 스케줄 제한 설정
    @PostMapping("/monthly-limit")
    public ResponseEntity<Response> setMonthlyLimit(
            @RequestBody SetMonthlyLimitRequest request,
            @RequestHeader(value = "userId", defaultValue = "1") Integer userId) {
        // @RequestHeader userId는 추후 인증로직이 추가되면 변경될 예정

        MonthlyScheduleConfig result = monthlyScheduleLimitService.setMonthlyLimit(
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
                        "해당 월의 스케줄 제한 설정을 찾을 수 없습니다.",
                        null
                )));
    }

    // 모든 월별 스케줄 제한 조회
    @GetMapping("/monthly-limits")
    public ResponseEntity<Response> getAllMonthlyLimits() {
        List<MonthlyScheduleConfig> limits = monthlyScheduleLimitService.getAllMonthlyLimits();

        return ResponseEntity.ok(Response.of(
                true,
                "모든 월별 스케줄 제한을 조회했습니다.",
                MonthlyLimitsResponse.from(limits)
        ));
    }
}