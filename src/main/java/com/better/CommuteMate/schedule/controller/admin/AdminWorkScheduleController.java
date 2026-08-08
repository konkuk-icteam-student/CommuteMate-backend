package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.AdminWorkScheduleQueryService;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminScheduleRangeResponse;
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

@Tag(name = "관리자 근로시간표", description = "관리자용 근로시간표 조회 API")
@RestController
@RequestMapping("/api/v1/admin/work-schedules")
@RequiredArgsConstructor
public class AdminWorkScheduleController {

    private final AdminWorkScheduleQueryService queryService;

    @GetMapping
    @Operation(summary = "근로시간표 조회", description = "같은 달 안의 근로시간표를 30분 슬롯 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "근로시간표 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "조회 성공",
                                            value = """
                                            {
                                              "isSuccess": true,
                                              "message": "근로시간표를 조회했습니다.",
                                              "details": {
                                                "startDate": "2026-04-15",
                                                "endDate": "2026-04-15",
                                                "maxConcurrentWorkers": 4,
                                                "hasPrev": true,
                                                "hasNext": true,
                                                "days": [{
                                                  "date": "2026-04-15",
                                                  "slots": [{
                                                    "start": "09:00",
                                                    "end": "09:30",
                                                    "status": "AVAILABLE",
                                                    "currentCount": 2,
                                                    "isOverLimit": false,
                                                    "users": [
                                                      {"userId": "1", "userName": "학생A"},
                                                      {"userId": "2", "userName": "학생B"}
                                                    ]
                                                  }]
                                                }]
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "해당 월 설정 없음",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "message": "근로시간표를 조회했습니다.",
                                                      "details": {
                                                        "startDate": "2026-04-15",
                                                        "endDate": "2026-04-15",
                                                        "maxConcurrentWorkers": 4,
                                                        "hasPrev": true,
                                                        "hasNext": false,
                                                        "days": []
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "조회 날짜가 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "조회 연도 또는 월 값이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getSchedules(
            @Parameter(description = "조회 시작일", example = "2026-04-15", required = true)
            @RequestParam(required = false) String startDate,
            @Parameter(description = "조회 종료일", example = "2026-04-15", required = true)
            @RequestParam(required = false) String endDate,
            @Parameter(description = "근무자 이름 검색", example = "학생A")
            @RequestParam(required = false) String userName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long organizationId = userDetails.getUser().getOrganizationId();
        AdminScheduleRangeResponse details = queryService.getSchedules(
                organizationId, startDate, endDate, userName
        );
        return ResponseEntity.ok(Response.of(
                true, "근로시간표를 조회했습니다.", details
        ));
    }
}
