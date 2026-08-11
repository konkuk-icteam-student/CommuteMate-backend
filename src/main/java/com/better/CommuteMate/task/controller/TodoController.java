package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.AdminTodoService;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
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
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Tag(name = "Todo", description = "업무사항 API")
public class TodoController {

    private final AdminTodoService adminTodoService;

    @GetMapping
    @Operation(summary = "일별 업무사항 조회", description = "특정 날짜의 업무사항을 오전과 오후로 구분하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업무사항 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "조회 결과 있음", value = SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "업무사항 없음", value = EMPTY_EXAMPLE)
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

    private static final String SUCCESS_EXAMPLE = """
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

    private static final String EMPTY_EXAMPLE = """
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
}
