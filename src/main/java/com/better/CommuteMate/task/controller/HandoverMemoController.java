package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.HandoverMemoService;
import com.better.CommuteMate.task.controller.dtos.HandoverMemosResponse;
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

@RestController
@RequestMapping("/api/admin/handover-memos")
@RequiredArgsConstructor
@Tag(name = "Admin Handover Memo", description = "관리자 인수인계 메모 API")
public class HandoverMemoController {

    private final HandoverMemoService handoverMemoService;

    @GetMapping
    @Operation(summary = "일별 인수인계 메모 조회", description = "인증된 사용자의 조직에 속한 특정 날짜 인수인계 메모를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인수인계 메모 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "조회 결과 있음", value = SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "메모 없음", value = EMPTY_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "날짜 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "날짜 형식이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMemos(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-04-15", required = true)
            @RequestParam String date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        HandoverMemosResponse details = handoverMemoService.getMemos(
                userDetails.getUser().getOrganizationId(), date
        );
        return ResponseEntity.ok(Response.of(
                true,
                "인수인계 메모 조회에 성공했습니다.",
                details
        ));
    }

    private static final String SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "인수인계 메모 조회에 성공했습니다.",
              "details": {
                "date": "2026-04-15",
                "memoCount": 1,
                "memos": [
                  {
                    "memoId": 1,
                    "content": "다음 근무자가 쓰레기봉투 꼭 갈아주세요.",
                    "createdBy": {
                      "userId": 7,
                      "name": "홍길동"
                    },
                    "createdAt": "2026-04-15T10:36:00"
                  }
                ]
              }
            }
            """;

    private static final String EMPTY_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "인수인계 메모 조회에 성공했습니다.",
              "details": {
                "date": "2026-04-15",
                "memoCount": 0,
                "memos": []
              }
            }
            """;
}
