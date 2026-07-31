package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.AdminWorkChangeRequestQueryService;
import com.better.CommuteMate.schedule.application.AdminWorkChangeRequestProcessService;
import com.better.CommuteMate.schedule.application.AdminWorkChangeRequestBulkService;
import com.better.CommuteMate.schedule.controller.admin.dtos.BulkApproveWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.BulkApproveWorkChangeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.WorkChangeRequestListResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 근로시간 수정 요청", description = "관리자의 근로시간 수정 요청 조회 API")
@RestController
@RequestMapping("/api/v1/admin/work-change-requests")
@RequiredArgsConstructor
public class AdminWorkChangeRequestController {

    private final AdminWorkChangeRequestQueryService queryService;
    private final AdminWorkChangeRequestProcessService processService;
    private final AdminWorkChangeRequestBulkService bulkService;

    @GetMapping
    @Operation(summary = "근로시간 수정 요청 목록 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "조회 결과 있음", value = SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "빈 요청 목록", value = EMPTY_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "조회 조건이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "상태 코드 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "올바르지 않은 변경 요청 상태입니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "페이지 요청 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "페이지 요청 값이 올바르지 않습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "연도 또는 월 오류",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "조회 연도 또는 월 값이 올바르지 않습니다.",
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
    public ResponseEntity<Response> getRequests(
            @Parameter(description = "조회 연도", example = "2026", required = true)
            @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 월", example = "6", required = true)
            @RequestParam(required = false) Integer month,
            @Parameter(description = "처리 상태: ALL, CS01, CS02, CS03", example = "CS01")
            @RequestParam(required = false) String statusCode,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(최대 100)", example = "10")
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        WorkChangeRequestListResponse details = queryService.getRequests(
                userDetails.getUser().getOrganizationId(),
                year,
                month,
                statusCode,
                page,
                size
        );
        return ResponseEntity.ok(Response.of(
                true,
                "근로시간 수정 요청 목록을 조회했습니다.",
                details
        ));
    }

    @PatchMapping("/{requestId}")
    @Operation(summary = "근로시간 수정 요청 승인/거절")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "승인 또는 거절 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "승인 성공", value = APPROVE_EXAMPLE),
                                    @ExampleObject(name = "거절 성공", value = REJECT_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "처리 요청이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "처리 상태 오류",
                                            value = """
                                                    {"isSuccess":false,"message":"올바르지 않은 처리 상태입니다.","details":null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "거절 사유 누락",
                                            value = """
                                                    {"isSuccess":false,"message":"거절 사유를 입력해야 합니다.","details":null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "이미 처리된 요청",
                                            value = """
                                                    {"isSuccess":false,"message":"이미 처리된 요청입니다.","details":null}
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "수정 요청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"근로시간 수정 요청을 찾을 수 없습니다.","details":null}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "승인 시 최대 근무 인원 초과",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"해당 시간대의 최대 근무 인원을 초과했습니다.","details":null}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> processRequest(
            @Parameter(description = "처리할 근로시간 수정 요청 ID", example = "1", required = true)
            @PathVariable Long requestId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "CS02 승인 또는 CS03 거절",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "승인",
                                            value = """
                                                    {"statusCode":"CS02","rejectReason":null}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "거절",
                                            value = """
                                                    {"statusCode":"CS03","rejectReason":"해당 시간대 정원이 초과되었습니다."}
                                                    """
                                    )
                            }
                    )
            )
            @RequestBody ProcessWorkChangeRequest command,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProcessWorkChangeResponse details = processService.process(
                requestId,
                command,
                userDetails.getUserId(),
                userDetails.getUser().getOrganizationId()
        );
        String message = "CS02".equals(details.statusCode)
                ? "근로시간 수정 요청을 승인했습니다."
                : "근로시간 수정 요청을 거절했습니다.";
        return ResponseEntity.ok(Response.of(true, message, details));
    }

    @PatchMapping("/bulk")
    @Operation(summary = "근로시간 수정 요청 일괄 승인")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일괄 승인 처리 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = BULK_APPROVE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 ID 목록이 올바르지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "요청 ID 목록이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> bulkApprove(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "승인할 근로시간 수정 요청 ID 목록",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"requestIds":[1,2,3]}
                                    """)
                    )
            )
            @RequestBody BulkApproveWorkChangeRequest command,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BulkApproveWorkChangeResponse details = bulkService.approve(
                command,
                userDetails.getUserId(),
                userDetails.getUser().getOrganizationId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "근로시간 수정 요청을 일괄 승인했습니다.",
                details
        ));
    }

    private static final String SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근로시간 수정 요청 목록을 조회했습니다.",
              "details": {
                "year": 2026,
                "month": 6,
                "statusCode": "CS01",
                "summary": {
                  "totalCount": 4,
                  "pendingCount": 2,
                  "approvedCount": 1,
                  "rejectedCount": 1
                },
                "requests": [{
                  "requestId": 1,
                  "userId": 2,
                  "userName": "김길동",
                  "statusCode": "CS01",
                  "requestedAt": "2026-06-13T10:20:00",
                  "processedAt": null,
                  "reason": "학과 행사 일정으로 인해 근무시간 변경을 요청합니다.",
                  "rejectReason": null,
                  "deleteSchedules": [{
                    "date": "2026-06-15",
                    "start": "09:00",
                    "end": "11:00",
                    "changeTypeCode": "CR02"
                  }],
                  "addSchedules": [{
                    "date": "2026-06-17",
                    "start": "13:00",
                    "end": "15:00",
                    "changeTypeCode": "CR01"
                  }]
                }],
                "page": 0,
                "size": 10,
                "totalElements": 4,
                "totalPages": 1
              }
            }
            """;

    private static final String EMPTY_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근로시간 수정 요청 목록을 조회했습니다.",
              "details": {
                "year": 2026,
                "month": 6,
                "statusCode": "CS01",
                "summary": {
                  "totalCount": 0,
                  "pendingCount": 0,
                  "approvedCount": 0,
                  "rejectedCount": 0
                },
                "requests": [],
                "page": 0,
                "size": 10,
                "totalElements": 0,
                "totalPages": 0
              }
            }
            """;

    private static final String APPROVE_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근로시간 수정 요청을 승인했습니다.",
              "details": {
                "requestId": 1,
                "statusCode": "CS02",
                "processedAt": "2026-06-13T14:30:00",
                "deleteSchedules": [{
                  "scheduleId": "uuid",
                  "date": "2026-06-15",
                  "start": "09:00",
                  "end": "11:00",
                  "statusCode": "WS04"
                }],
                "addSchedules": [{
                  "scheduleId": "uuid",
                  "date": "2026-06-17",
                  "start": "13:00",
                  "end": "15:00",
                  "statusCode": "WS02"
                }]
              }
            }
            """;

    private static final String REJECT_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근로시간 수정 요청을 거절했습니다.",
              "details": {
                "requestId": 1,
                "statusCode": "CS03",
                "processedAt": "2026-06-13T14:30:00",
                "rejectReason": "해당 시간대 정원이 초과되었습니다."
              }
            }
            """;

    private static final String BULK_APPROVE_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근로시간 수정 요청을 일괄 승인했습니다.",
              "details": {
                "summary": {
                  "totalCount": 3,
                  "successCount": 2,
                  "failCount": 1
                },
                "results": [
                  {
                    "requestId": 1,
                    "resultCode": "SUCCESS",
                    "processedAt": "2026-06-13T14:30:00"
                  },
                  {
                    "requestId": 2,
                    "resultCode": "SUCCESS",
                    "processedAt": "2026-06-13T14:30:00"
                  },
                  {
                    "requestId": 3,
                    "resultCode": "CAPACITY_EXCEEDED",
                    "processedAt": null
                  }
                ]
              }
            }
            """;
}
