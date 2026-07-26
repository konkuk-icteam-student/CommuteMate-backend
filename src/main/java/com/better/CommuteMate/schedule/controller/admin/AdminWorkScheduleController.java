package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.AdminWorkScheduleQueryService;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminScheduleRangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 근로시간표", description = "관리자용 근로시간표 조회 API")
@RestController
@RequestMapping("/api/v1/admin/work-schedules")
@RequiredArgsConstructor
public class AdminWorkScheduleController {

    private final AdminWorkScheduleQueryService queryService;

    @GetMapping
    @Operation(summary = "근로 시간표 조회", description = "같은 달 안의 근로시간표를 30분 슬롯 단위로 조회합니다.")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getSchedules(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String userName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String organizationId = String.valueOf(userDetails.getUser().getOrganizationId());
        AdminScheduleRangeResponse details = queryService.getSchedules(
                organizationId, startDate, endDate, userName
        );
        return ResponseEntity.ok(Response.of(
                true, "근로시간표를 조회했습니다.", details
        ));
    }
}
