package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.AdminHomeService;
import com.better.CommuteMate.home.controller.dto.AdminAttendanceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 홈", description = "관리자 홈 화면 API")
@RestController
@RequestMapping("/api/v1/admin/home")
@RequiredArgsConstructor
public class AdminHomeController {

    private final AdminHomeService adminHomeService;

    @GetMapping("/attendance-summary")
    @Operation(
            summary = "오늘 근로현황 조회",
            description = "지정한 날짜의 현재 근무 중, 미출근, 지각 인원과 업무 완료 현황을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "오늘 근로현황 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "오늘 근로현황을 조회했습니다.",
                                              "details": {
                                                "date": "2026-04-15",
                                                "currentWorkingCount": 3,
                                                "notCheckedInCount": 3,
                                                "lateCount": 3,
                                                "todayTask": {
                                                  "completedCount": 1,
                                                  "totalCount": 6
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "조회 날짜 값이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "잘못된 날짜",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "message": "조회 날짜 값이 올바르지 않습니다.",
                                              "details": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getAttendanceSummary(
            @Parameter(
                    description = "조회 날짜 (yyyy-MM-dd)",
                    example = "2026-04-15",
                    required = true
            )
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long organizationId = userDetails.getUser().getOrganizationId();
        AdminAttendanceSummaryResponse details =
                adminHomeService.getAttendanceSummary(organizationId, date);
        return ResponseEntity.ok(Response.of(
                true,
                "오늘 근로현황을 조회했습니다.",
                details
        ));
    }

}
