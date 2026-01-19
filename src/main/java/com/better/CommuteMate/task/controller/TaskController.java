package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.TaskService;
import com.better.CommuteMate.task.controller.dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task", description = "업무 관리 API")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "일별 업무 목록 조회", description = "특정 날짜의 업무 목록을 정기/비정기, 오전/오후로 구분하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = DailyTasksResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식")
    })
    @GetMapping
    public ResponseEntity<Response> getTasksByDate(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2025-10-24") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyTasksResponse response = taskService.getTasksByDate(date);
        return ResponseEntity.ok(new Response(true, "업무 목록을 조회했습니다.", response));
    }

    @Operation(summary = "업무 단건 조회", description = "특정 업무의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "업무를 찾을 수 없음")
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<Response> getTask(
            @PathVariable Long taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(new Response(true, "업무를 조회했습니다.", response));
    }

    @Operation(summary = "업무 생성", description = "새로운 업무를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "담당자를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<Response> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUser().getUserId();
        TaskResponse response = taskService.createTask(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response(true, "업무가 생성되었습니다.", response));
    }

    @Operation(summary = "업무 수정", description = "기존 업무의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "업무 또는 담당자를 찾을 수 없음")
    })
    @PatchMapping("/{taskId}")
    public ResponseEntity<Response> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUser().getUserId();
        TaskResponse response = taskService.updateTask(taskId, request, currentUserId);
        return ResponseEntity.ok(new Response(true, "업무가 수정되었습니다.", response));
    }

    @Operation(summary = "업무 완료 상태 토글", description = "업무의 완료 상태를 토글합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토글 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "업무를 찾을 수 없음")
    })
    @PatchMapping("/{taskId}/toggle-complete")
    public ResponseEntity<Response> toggleComplete(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUser().getUserId();
        TaskResponse response = taskService.toggleComplete(taskId, currentUserId);
        String message = response.getIsCompleted() ? "업무를 완료 처리했습니다." : "업무를 미완료 처리했습니다.";
        return ResponseEntity.ok(new Response(true, message, response));
    }

    @Operation(summary = "업무 완료 상태 설정", description = "업무의 완료 상태를 특정 값으로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "업무를 찾을 수 없음")
    })
    @PatchMapping("/{taskId}/complete")
    public ResponseEntity<Response> setComplete(
            @PathVariable Long taskId,
            @Valid @RequestBody CompleteTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUser().getUserId();
        TaskResponse response = taskService.setComplete(taskId, request.getIsCompleted(), currentUserId);
        String message = response.getIsCompleted() ? "업무를 완료 처리했습니다." : "업무를 미완료 처리했습니다.";
        return ResponseEntity.ok(new Response(true, message, response));
    }

    @Operation(summary = "업무 삭제", description = "업무를 삭제합니다. (관리자 전용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "업무를 찾을 수 없음")
    })
    @PreAuthorize("hasRole('RL02')")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Response> deleteTask(
            @PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(new Response(true, "업무가 삭제되었습니다.", null));
    }

    @Operation(summary = "업무 일괄 저장", description = "여러 업무를 한 번에 생성하거나 수정합니다. taskId가 있으면 수정, 없으면 생성합니다. (관리자 전용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "저장 성공 (일부 실패 포함)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('RL02')")
    @PutMapping("/batch")
    public ResponseEntity<Response> batchUpdateTasks(
            @Valid @RequestBody BatchUpdateTasksRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUser().getUserId();
        BatchUpdateTasksResponse response = taskService.batchUpdateTasks(request, currentUserId);

        String message;
        HttpStatus status;
        if (response.getTotalErrors() == 0) {
            message = String.format("업무가 저장되었습니다. (생성: %d개, 수정: %d개)",
                    response.getTotalCreated(), response.getTotalUpdated());
            status = HttpStatus.OK;
        } else if (response.getTotalCreated() + response.getTotalUpdated() > 0) {
            message = String.format("일부 업무 저장에 실패했습니다. (성공: %d개, 실패: %d개)",
                    response.getTotalCreated() + response.getTotalUpdated(), response.getTotalErrors());
            status = HttpStatus.MULTI_STATUS;
        } else {
            message = "모든 업무 저장에 실패했습니다.";
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        }

        return ResponseEntity.status(status)
                .body(new Response(response.getTotalErrors() == 0, message, response));
    }
}
