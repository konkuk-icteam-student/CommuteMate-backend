package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.AdminTodoService;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoResponse;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/todos")
@RequiredArgsConstructor
@Tag(name = "Admin Todo", description = "관리자 업무사항 API")
public class AdminTodoController {

    private final AdminTodoService adminTodoService;

    @PostMapping
    @PreAuthorize("hasRole('RL02')")
    @Operation(
            summary = "관리자 업무사항 등록",
            description = "특정 날짜와 시간에 수행할 업무사항을 등록합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "date": "2026-04-15",
                                      "timeSlot": "09:00",
                                      "description": "신문지 가져오기"
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "업무사항 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CREATE_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "업무사항 입력값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "업무사항 입력값이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> createTodo(
            @Valid @RequestBody CreateAdminTodoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateAdminTodoResponse details = adminTodoService.createTodo(
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "업무사항 등록에 성공했습니다.",
                details
        ));
    }

    @PatchMapping("/{todoId}")
    @PreAuthorize("hasRole('RL02')")
    @Operation(
            summary = "관리자 업무사항 수정",
            description = "업무 날짜, 시간, 내용 중 전달된 필드만 수정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "date": "2026-04-15",
                                      "timeSlot": "14:00",
                                      "description": "회의실 청소"
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업무사항 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = UPDATE_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "업무사항 입력값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "업무사항 입력값이 올바르지 않습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(
                    responseCode = "403",
                    description = "업무사항 수정 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "업무사항을 수정할 권한이 없습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
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
    public ResponseEntity<Response> updateTodo(
            @Parameter(description = "수정할 업무사항 ID", example = "1", required = true)
            @PathVariable Long todoId,
            @Valid @RequestBody UpdateAdminTodoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateAdminTodoResponse details = adminTodoService.updateTodo(
                todoId,
                request,
                userDetails.getUserId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "업무사항 수정에 성공했습니다.",
                details
        ));
    }

    @DeleteMapping("/{todoId}")
    @PreAuthorize("hasRole('RL02')")
    @Operation(
            summary = "관리자 업무사항 삭제",
            description = "업무사항을 삭제합니다. 같은 조직의 관리자만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "업무사항 삭제 성공",
                    content = @Content
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
                    description = "업무사항 삭제 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "message": "업무사항을 삭제할 권한이 없습니다.",
                                      "details": null
                                    }
                                    """)
                    )
            ),
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
    public ResponseEntity<Void> deleteTodo(
            @Parameter(description = "삭제할 업무사항 ID", example = "1", required = true)
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminTodoService.deleteTodo(todoId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('RL02')")
    @Operation(summary = "관리자 일별 업무사항 조회", description = "특정 날짜의 업무사항을 오전과 오후로 구분하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업무사항 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "조회 결과 있음",
                                            value = SUCCESS_EXAMPLE
                                    ),
                                    @ExampleObject(
                                            name = "업무사항 없음",
                                            value = EMPTY_EXAMPLE
                                    )
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
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
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

    private static final String CREATE_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "업무사항 등록에 성공했습니다.",
              "details": {
                "todoId": 1,
                "date": "2026-04-15",
                "timeSlot": "09:00:00",
                "description": "신문지 가져오기",
                "status": "PENDING",
                "completed": false,
                "createdAt": "2026-04-15T08:30:00"
              }
            }
            """;

    private static final String UPDATE_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "업무사항 수정에 성공했습니다.",
              "details": {
                "todoId": 1,
                "date": "2026-04-15",
                "timeSlot": "14:00:00",
                "description": "회의실 청소",
                "status": "PENDING",
                "completed": false,
                "updatedAt": "2026-04-15T10:40:00"
              }
            }
            """;
}
