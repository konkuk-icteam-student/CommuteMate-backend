package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.AdminTodoService;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
import com.better.CommuteMate.task.controller.dtos.UpdateTodoCompletionRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateTodoCompletionResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Tag(name = "Todo", description = "업무사항 API")
public class TodoController {

    private final AdminTodoService adminTodoService;

    @GetMapping
    @Operation(summary = "일별 업무사항 조회", description = "매일 반복되는 고정 업무와 조회 날짜의 완료 상태를 오전과 오후로 구분하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업무사항 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "조회 결과 있음", value = GET_SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "업무사항 없음", value = GET_EMPTY_EXAMPLE)
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
    public ResponseEntity<Response> getTodos(
            @Parameter(description = "조회할 업무 날짜 (yyyy-MM-dd)", example = "2026-04-15", required = true)
            @RequestParam String date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminTodosResponse details = adminTodoService.getTodos(
                userDetails.getUser().getOrganizationId(), date
        );
        return ResponseEntity.ok(Response.of(true, "업무사항 조회에 성공했습니다.", details));
    }

    @PatchMapping("/{todoId}/completion")
    @Operation(
            summary = "업무사항 완료 체크",
            description = "지정한 날짜의 업무 완료 여부를 변경합니다. 다른 날짜의 완료 상태에는 영향을 주지 않습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "date": "2026-04-15", "isCompleted": true }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "완료 여부 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CHECK_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "완료 여부 값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "완료 여부 값이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "업무사항을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "업무사항을 찾을 수 없습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            )
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> checkTodo(
            @Parameter(description = "완료 여부를 변경할 업무사항 ID", example = "1", required = true)
            @PathVariable Long todoId,
            @Valid @RequestBody UpdateTodoCompletionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateTodoCompletionResponse details = adminTodoService.checkTodo(
                todoId,
                request.date(),
                request.isCompleted(),
                userDetails.getUserId(),
                userDetails.getUser().getOrganizationId(),
                userDetails.getUser().getName()
        );
        return ResponseEntity.ok(Response.of(true, "업무사항 완료 여부를 변경했습니다.", details));
    }

    private static final String GET_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "업무사항 조회에 성공했습니다.",
              "details": {
                "date": "2026-04-15",
                "morningTodos": [
                  {
                    "todoId": 1,
                    "description": "신문지 가져오기",
                    "timeSlot": "09:00:00",
                    "status": "COMPLETED",
                    "createdBy": {
                      "userId": 7,
                      "name": "홍길동"
                    },
                    "createdAt": "2026-04-15T08:50:00",
                    "completedByName": "홍길동",
                    "completedTime": "09:13:00"
                  },
                  {
                    "todoId": 2,
                    "description": "커피머신 청소",
                    "timeSlot": "10:00:00",
                    "status": "PENDING",
                    "createdBy": {
                      "userId": 7,
                      "name": "홍길동"
                    },
                    "createdAt": "2026-04-15T08:55:00",
                    "completedByName": null,
                    "completedTime": null
                  }
                ],
                "afternoonTodos": [
                  {
                    "todoId": 3,
                    "description": "회의실 청소",
                    "timeSlot": "14:00:00",
                    "status": "PENDING",
                    "createdBy": {
                      "userId": 7,
                      "name": "홍길동"
                    },
                    "createdAt": "2026-04-15T09:00:00",
                    "completedByName": null,
                    "completedTime": null
                  }
                ]
              }
            }
            """;

    private static final String GET_EMPTY_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "업무사항 조회에 성공했습니다.",
              "details": {
                "date": "2026-04-15",
                "morningTodos": [],
                "afternoonTodos": []
              }
            }
            """;

    private static final String CHECK_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "업무사항 완료 여부를 변경했습니다.",
              "details": {
                "date": "2026-04-15",
                "todo": {
                  "todoId": 2,
                  "description": "커피머신 청소",
                  "timeSlot": "09:00",
                  "status": "COMPLETED",
                  "completedByName": "홍길동",
                  "completedTime": "10:36"
                },
                "summary": { "completedCount": 3, "totalCount": 4 }
              }
            }
            """;
}
