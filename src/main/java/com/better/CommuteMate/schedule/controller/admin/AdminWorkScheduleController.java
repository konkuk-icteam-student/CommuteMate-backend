package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.AdminWorkAssignmentService;
import com.better.CommuteMate.schedule.application.AdminWorkScheduleDeletionService;
import com.better.CommuteMate.schedule.application.AdminWorkScheduleQuickSearchService;
import com.better.CommuteMate.schedule.application.AdminWorkScheduleQueryService;
import com.better.CommuteMate.schedule.application.AdminUserWorkScheduleQueryService;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkAssignmentRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkAssignmentResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminScheduleRangeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleDeleteResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleQuickSearchResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "관리자 근로시간표", description = "근로시간표 조회 API")
@RestController
@RequestMapping("/api/v1/admin/work-schedules")
@RequiredArgsConstructor
public class AdminWorkScheduleController {

    private final AdminWorkScheduleQueryService queryService;
    private final AdminWorkAssignmentService assignmentService;
    private final AdminWorkScheduleDeletionService deletionService;
    private final AdminWorkScheduleQuickSearchService quickSearchService;
    private final AdminUserWorkScheduleQueryService userScheduleQueryService;

    @GetMapping("/user")
    @Operation(
            summary = "사용자별 근로 시간표 조회",
            description = "관리자가 같은 조직에 소속된 사용자의 근로 시간표를 지정한 날짜 범위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 근로 시간표 조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "message": "홍길동의 근로 시간표를 조회했습니다.",
                              "details": {
                                "startDate": "2026-05-18",
                                "endDate": "2026-05-22",
                                "maxConcurrentWorkers": 10,
                                "totalLimitHours": 27,
                                "usedHours": 10,
                                "days": [
                                  {
                                    "date": "2026-05-18",
                                    "slots": [
                                      {
                                        "start": "13:00",
                                        "end": "13:30",
                                        "status": "MY_SCHEDULE",
                                        "currentCount": 3
                                      },
                                      {
                                        "start": "13:30",
                                        "end": "14:00",
                                        "status": "EMPTY",
                                        "currentCount": 0
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "조회 기간이 올바르지 않음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "시작일이 종료일보다 늦음", value = """
                                    {
                                      "isSuccess": false,
                                      "message": "시작 날짜는 종료 날짜보다 늦을 수 없습니다.",
                                      "details": null
                                    }
                                    """),
                            @ExampleObject(name = "서로 다른 달", value = """
                                    {
                                      "isSuccess": false,
                                      "message": "조회 기간은 같은 달 이내여야 합니다.",
                                      "details": null
                                    }
                                    """)
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "message": "사용자를 찾을 수 없습니다.",
                              "details": null
                            }
                            """))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getUserSchedule(
            @Parameter(description = "조회할 사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @Parameter(description = "조회 시작일", example = "2026-05-18", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일", example = "2026-05-22", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminUserWorkScheduleQueryService.Result result = userScheduleQueryService.getSchedule(
                userId,
                userDetails.getUser().getOrganizationId(),
                startDate,
                endDate
        );
        WorkScheduleRangeResponse details = result.response();
        return ResponseEntity.ok(Response.of(
                true, result.userName() + "의 근로 시간표를 조회했습니다.", details
        ));
    }

    @PostMapping
    @Operation(
            summary = "근로 시간표 직접 배치",
            description = """
                    관리자가 소속 조직의 사용자를 30분 근로 슬롯에 직접 배치합니다.
                    생성된 일정은 승인 절차 없이 WS02(승인) 상태가 되며,
                    현재 인원이 최대 동시 근무 인원을 초과하더라도 배치할 수 있습니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "근로 시간표 배치 요청",
                                    value = """
                                            {
                                              "userId": "1",
                                              "date": "2026-09-08",
                                              "startTime": "09:00",
                                              "endTime": "09:30"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "근로 시간표 추가 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "배치 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "근로 시간표가 추가되었습니다.",
                                              "details": {
                                                "scheduleId": "9f36a98d-1377-4f44-86bb-f87ff47e39ac",
                                                "userId": "1",
                                                "userName": "홍길동",
                                                "date": "2026-09-08",
                                                "startTime": "09:00",
                                                "endTime": "09:30",
                                                "currentCount": 3,
                                                "maxConcurrentWorkers": 4
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "시간 형식 오류 또는 사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "30분 단위가 아닌 시간",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "근로 시간은 30분 단위로만 지정할 수 있습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "사용자 없음 또는 다른 조직 소속",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "사용자를 찾을 수 없습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "사용자가 해당 슬롯에 이미 배치됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "중복 배치",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "message": "이미 해당 시간에 배치된 사용자입니다.",
                                              "details": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 월의 스케줄 설정 또는 조직의 근무지를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "월별 스케줄 설정 없음",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "해당 월의 스케줄 설정을 찾을 수 없습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "조직 근무지 없음",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "조직의 근무지를 찾을 수 없습니다.",
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
    public ResponseEntity<Response> assignSchedule(
            @Valid @RequestBody AdminWorkAssignmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long organizationId = userDetails.getUser().getOrganizationId();
        Long adminId = userDetails.getUser().getUserId();
        AdminWorkAssignmentResponse details = assignmentService.assign(
                request, organizationId, adminId
        );
        return ResponseEntity.ok(Response.of(
                true, "근로 시간표가 추가되었습니다.", details
        ));
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(
            summary = "근로 시간표 삭제",
            description = """
                    관리자가 소속 조직의 근로 시간표를 삭제합니다.
                    실제 행은 삭제하지 않고 상태를 WS04(취소)로 변경합니다.
                    출퇴근 기록이 있는 스케줄은 삭제할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "근로 시간표 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "삭제 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "근로 시간표가 삭제되었습니다.",
                                              "details": {
                                                "scheduleId": "e5f6a7b8-1f2c-4d3e-9a5b-6c7d8e9f0123",
                                                "date": "2026-09-08",
                                                "startTime": "09:00",
                                                "endTime": "09:30",
                                                "currentCount": 2,
                                                "maxConcurrentWorkers": 4
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "스케줄 또는 해당 월의 설정을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "근로 시간표 없음",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "근로 시간표를 찾을 수 없습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "월별 스케줄 설정 없음",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "해당 월의 스케줄 설정을 찾을 수 없습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 출퇴근 기록이 있는 스케줄",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "출퇴근 기록 존재",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "message": "출퇴근 기록이 있어 삭제할 수 없습니다.",
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
    public ResponseEntity<Response> deleteSchedule(
            @Parameter(description = "삭제할 스케줄 ID", example = "e5f6a7b8-1f2c-4d3e-9a5b-6c7d8e9f0123", required = true)
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminWorkScheduleDeleteResponse details = deletionService.delete(
                scheduleId,
                userDetails.getUser().getOrganizationId(),
                userDetails.getUser().getUserId()
        );
        return ResponseEntity.ok(Response.of(
                true, "근로 시간표가 삭제되었습니다.", details
        ));
    }

    @GetMapping("/quick-search")
    @Operation(
            summary = "사용자 근로 시간표 빠른 조회",
            description = """
                    같은 조직 사용자의 승인(WS02) 근로 시간표를 기간 내에서 조회합니다.
                    연속되거나 겹치는 슬롯은 하나의 구간으로 병합하며,
                    배치가 없는 날짜는 응답에서 제외합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "근로 시간표 빠른 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "요청에 성공하였습니다.",
                                              "details": {
                                                "userId": "2",
                                                "userName": "박영희",
                                                "days": [
                                                  {
                                                    "date": "2026-09-10",
                                                    "dayOfWeek": "목",
                                                    "slots": [
                                                      {"start": "09:30", "end": "11:30"}
                                                    ]
                                                  },
                                                  {
                                                    "date": "2026-09-11",
                                                    "dayOfWeek": "금",
                                                    "slots": [
                                                      {"start": "16:30", "end": "18:30"}
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "사용자를 찾을 수 없거나 조회 기간이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "사용자 없음 또는 다른 조직 소속",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "사용자를 찾을 수 없습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "조회 기간 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "조회 기간이 올바르지 않습니다.",
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
    public ResponseEntity<Response> quickSearch(
            @Parameter(description = "조회할 사용자 ID", example = "2", required = true)
            @RequestParam(required = false) String userId,
            @Parameter(description = "조회 시작일 (YYYY-MM-DD)", example = "2026-09-07", required = true)
            @RequestParam(required = false) String startDate,
            @Parameter(description = "조회 종료일 (YYYY-MM-DD)", example = "2026-09-11", required = true)
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminWorkScheduleQuickSearchResponse details = quickSearchService.search(
                userId,
                startDate,
                endDate,
                userDetails.getUser().getOrganizationId()
        );
        return ResponseEntity.ok(Response.of(
                true, "요청에 성공하였습니다.", details
        ));
    }

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
                                                      {"userId": "1", "userName": "학생A", "scheduleId": 101, "workStatusCode": "WK03"},
                                                      {"userId": "2", "userName": "학생B", "scheduleId": 102, "workStatusCode": "WK01"}
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
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
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
