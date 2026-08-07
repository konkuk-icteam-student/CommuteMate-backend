package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.MonthlyScheduleSettingService;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 근무 일정 관리", description = "관리자용 근무 일정 설정 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final MonthlyScheduleSettingService monthlyScheduleSettingService;

    @PutMapping("/work-application-settings/{year}/{month}")
    @Operation(
            summary = "월별 근로신청 설정 저장",
            description = "설정을 생성 또는 수정하고 새 규칙에 맞지 않는 기존 신청을 취소합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "근로신청 설정 저장 요청",
                                    value = """
                                            {
                                              "applyStartDate": "2026-04-01",
                                              "applyEndDate": "2026-04-10",
                                              "unavailableDates": ["2026-04-19"],
                                              "unavailableTimeRanges": [
                                                {"start": "11:00", "end": "13:00"}
                                              ],
                                              "maxConcurrentWorkers": 4,
                                              "minWorkUnitMinutes": 120,
                                              "weeklyMinMinutes": 300,
                                              "weeklyMaxMinutes": 540,
                                              "monthlyMinMinutes": 1200,
                                              "monthlyMaxMinutes": 1620
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "근로신청 설정 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "저장 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "근로신청 설정을 저장했습니다.",
                                              "details": {
                                                "year": 2026,
                                                "month": 4,
                                                "applyStartDate": "2026-04-01",
                                                "applyEndDate": "2026-04-10",
                                                "maxConcurrentWorkers": 4,
                                                "minWorkUnitMinutes": 120,
                                                "weeklyMinMinutes": 300,
                                                "weeklyMaxMinutes": 540,
                                                "monthlyMinMinutes": 1200,
                                                "monthlyMaxMinutes": 1620,
                                                "unavailableDates": ["2026-04-19"],
                                                "unavailableTimeRanges": [
                                                  {"start": "11:00", "end": "13:00"}
                                                ],
                                                "affectedScheduleCount": 12,
                                                "affectedUserCount": 5
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "필수값 누락 또는 형식 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "요청 값이 올바르지 않습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "신청 기간 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "근로신청 시작일은 마감일보다 이전이어야 합니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "최소·최대 근무시간 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "최소 근무시간은 최대 근무시간보다 작아야 합니다.",
                                                      "details": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> saveScheduleSetting(
            @Parameter(description = "설정 연도", example = "2026", required = true)
            @PathVariable int year,
            @Parameter(description = "설정 월", example = "4", required = true)
            @PathVariable int month,
            @Valid @RequestBody SaveScheduleSettingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long organizationId = userDetails.getUser().getOrganizationId();
        String adminId = String.valueOf(userDetails.getUserId());
        SaveScheduleSettingResponse result = monthlyScheduleSettingService.save(
                organizationId, year, month, request, adminId
        );
        return ResponseEntity.ok(Response.of(
                true,
                "근로신청 설정을 저장했습니다.",
                result
        ));
    }
}
