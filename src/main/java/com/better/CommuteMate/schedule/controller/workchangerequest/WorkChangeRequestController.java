package com.better.CommuteMate.schedule.controller.workchangerequest;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.WorkChangeRequestHistoryService;
import com.better.CommuteMate.schedule.controller.workchangerequest.dtos.WorkChangeRequestHistoryResponse;
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

@Tag(name = "사용자 근로시간 수정 신청", description = "사용자의 근로시간 수정 신청기록 조회 API")
@SecurityRequirement(name = "JWT")
@RestController
@RequestMapping("/api/v1/work-change-requests")
@RequiredArgsConstructor
public class WorkChangeRequestController {

    private final WorkChangeRequestHistoryService historyService;

    @GetMapping("/history")
    @Operation(
            summary = "근무시간 수정 신청기록 조회",
            description = "로그인한 사용자의 근무시간 변경 신청기록을 조회합니다. " +
                    "조회 연월, 신청 상태, 페이지 조건을 기준으로 신청 내역을 반환하며, " +
                    "신청기록은 신청 시각 기준 최신순으로 정렬됩니다. " +
                    "year와 month를 모두 지정하면 해당 연월로 필터링하고, 하나만 있거나 둘 다 없으면 전체 기간을 조회합니다. " +
                    "summary는 상태 필터와 관계없이 조회 연월의 전체 신청 현황을 기준으로 계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "신청기록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "조회 결과 있음", value = SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "빈 신청기록", value = EMPTY_EXAMPLE)
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
                                                      "message": "올바르지 않은 신청기록 상태입니다.",
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
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    public ResponseEntity<Response> getHistory(
            @Parameter(description = "조회 연도. year와 month 모두 있을 때만 연월 필터 적용됩니다.", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 월. year와 month 모두 있을 때만 연월 필터 적용됩니다.", example = "4")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "신청 상태 필터: ALL(전체, 기본값), CS01(대기), CS02(승인), CS03(거절)", example = "ALL")
            @RequestParam(required = false) String statusCode,
            @Parameter(description = "페이지 번호 (기본값: 0)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지당 항목 수 (기본값: 10)", example = "10")
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        WorkChangeRequestHistoryResponse details = historyService.getHistory(
                userDetails.getUserId(),
                year,
                month,
                statusCode,
                page,
                size
        );
        return ResponseEntity.ok(Response.of(
                true,
                "근무시간 신청기록을 조회했습니다.",
                details
        ));
    }

    private static final String SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "근무시간 신청기록을 조회했습니다.",
              "details": {
                "year": 2026,
                "month": 4,
                "statusCode": "ALL",
                "summary": {
                  "totalCount": 4,
                  "approvedCount": 2,
                  "pendingCount": 1,
                  "rejectedCount": 1
                },
                "histories": [
                  {
                    "requestId": 1,
                    "statusCode": "CS01",
                    "statusName": "대기",
                    "requestedAt": "2026-04-01T10:00:00",
                    "processedAt": null,
                    "reason": "사유 입력",
                    "rejectReason": null,
                    "deleteSlots": [
                      { "start": "2026-04-06T13:00:00", "end": "2026-04-06T14:30:00", "changeTypeCode": "CR02" }
                    ],
                    "addSlots": [
                      { "start": "2026-04-09T13:00:00", "end": "2026-04-09T14:30:00", "changeTypeCode": "CR01" }
                    ]
                  }
                ],
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
              "message": "근무시간 신청기록을 조회했습니다.",
              "details": {
                "year": 2026,
                "month": 4,
                "statusCode": "ALL",
                "summary": {
                  "totalCount": 0,
                  "approvedCount": 0,
                  "pendingCount": 0,
                  "rejectedCount": 0
                },
                "histories": [],
                "page": 0,
                "size": 10,
                "totalElements": 0,
                "totalPages": 0
              }
            }
            """;
}
