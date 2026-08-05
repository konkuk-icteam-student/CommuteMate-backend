package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.AdminHomeService;
import com.better.CommuteMate.home.application.AdminUserAttendanceService;
import com.better.CommuteMate.home.controller.dto.AdminAttendanceSummaryResponse;
import com.better.CommuteMate.home.controller.dto.AdminUserAttendancePageResponse;
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
    private final AdminUserAttendanceService adminUserAttendanceService;

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
            @RequestParam String date,
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

    @GetMapping("/attendance-status")
    @Operation(
            summary = "인원별 근태 현황 조회",
            description = """
                    지정 날짜 기준 조직 구성원의 근무·근태 상태와 해당 주/월 누적 근무시간을 조회합니다.
                    근무 상태: WK01 근무 예정, WK02 근무 중, WK03 근무 완료, WK04 미출근.
                    근태 상태: AT01 정상, AT02 지각(예정 시작 10분 초과 후 출근), AT03 결근.
                    근태 상태가 없거나 해당 날짜에 근무 일정이 없으면 코드가 null입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인원별 근태 현황 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "message": "인원별 근태 현황을 조회했습니다.",
                                      "details": {
                                        "date": "2026-04-15",
                                        "users": [{
                                          "userId": "1",
                                          "userName": "최지훈",
                                          "department": "정보보호학부",
                                          "studentId": "202311306",
                                          "workStatusCode": "WK02",
                                          "attendanceStatusCode": "AT02",
                                          "lateCount": 1,
                                          "lateMinutes": 18,
                                          "weeklyWorkedMinutes": 270,
                                          "weeklyLimitMinutes": 540,
                                          "monthlyWorkedMinutes": 810,
                                          "monthlyLimitMinutes": 1620
                                        }],
                                        "page": 0,
                                        "size": 6,
                                        "totalElements": 18,
                                        "totalPages": 3
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "날짜 또는 페이지 요청 값이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "잘못된 날짜", value = """
                                            {"isSuccess":false,"message":"조회 날짜 값이 올바르지 않습니다.","details":null}
                                            """),
                                    @ExampleObject(name = "잘못된 페이지", value = """
                                            {"isSuccess":false,"message":"페이지 요청 값이 올바르지 않습니다.","details":null}
                                            """)
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getUserAttendance(
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-04-15", required = true)
            @RequestParam(required = false) String date,
            @Parameter(description = "이름 검색(부분 일치)", example = "김")
            @RequestParam(required = false) String userName,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "6")
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminUserAttendancePageResponse details =
                adminUserAttendanceService.getUserAttendance(
                        userDetails.getUser().getOrganizationId(),
                        date,
                        userName,
                        page,
                        size
                );
        return ResponseEntity.ok(Response.of(
                true,
                "인원별 근태 현황을 조회했습니다.",
                details
        ));
    }

}
