package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.HandoverMemoService;
import com.better.CommuteMate.task.controller.dtos.CreateHandoverMemoRequest;
import com.better.CommuteMate.task.controller.dtos.CreateHandoverMemoResponse;
import com.better.CommuteMate.task.controller.dtos.HandoverMemosResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/handover-memos")
@RequiredArgsConstructor
@Tag(name = "Handover Memo", description = "인수인계 메모 API")
public class HandoverMemoController {

    private final HandoverMemoService handoverMemoService;

    @PostMapping
    @Operation(
            summary = "인수인계 메모 작성",
            description = "인증된 사용자의 조직에 인수인계 메모를 작성합니다. 메모는 작성일로부터 3일 후 만료됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "content": "다음 근무자가 쓰레기봉투 꼭 갈아주세요."
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인수인계 메모 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CREATE_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "메모 내용 누락",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "메모 내용을 입력해야 합니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> createMemo(
            @Valid @RequestBody CreateHandoverMemoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateHandoverMemoResponse details = handoverMemoService.createMemo(
                userDetails.getUser().getOrganizationId(),
                userDetails.getUser(),
                request
        );
        return ResponseEntity.ok(Response.of(
                true,
                "인수인계 메모를 작성했습니다.",
                details
        ));
    }

    @DeleteMapping("/{memoId}")
    @Operation(summary = "인수인계 메모 삭제", description = "본인이 작성한 인수인계 메모를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인수인계 메모 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = DELETE_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "인증이 필요합니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "본인 메모 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "본인이 작성한 메모만 삭제할 수 있습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "메모 없음 또는 이미 삭제됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "인수인계 메모를 찾을 수 없습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> deleteMemo(
            @Parameter(description = "삭제할 메모 ID", example = "1", required = true)
            @PathVariable Long memoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        handoverMemoService.deleteMemo(memoId, userDetails.getUserId());
        return ResponseEntity.ok(new Response<>(true, "인수인계 메모를 삭제했습니다.", null));
    }

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

    private static final String DELETE_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "인수인계 메모를 삭제했습니다.",
              "details": null
            }
            """;

    private static final String CREATE_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "인수인계 메모를 작성했습니다.",
              "details": {
                "memoId": 1,
                "content": "다음 근무자가 쓰레기봉투 꼭 갈아주세요.",
                "createdBy": {
                  "userId": 7,
                  "name": "홍길동"
                },
                "createdAt": "2026-04-15 10:36",
                "expiresAt": "2026-04-18 10:36"
              }
            }
            """;

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
