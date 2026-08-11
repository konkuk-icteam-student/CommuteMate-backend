package com.better.CommuteMate.admin.controller;

import com.better.CommuteMate.admin.application.AdminWorkerService;
import com.better.CommuteMate.admin.controller.dto.AdminWorkerPageResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 근무 인원 관리", description = "관리자 근무 인원 조회 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminWorkerController {
    private final AdminWorkerService adminWorkerService;

    @GetMapping("/workers")
    @Operation(summary = "근무 인원 목록 조회", description = "이름으로 근무자를 검색하고 기준일이 속한 주·월의 근무 및 요청·근태 통계를 조회합니다. 시간 값은 분 단위입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근무 인원 목록 조회 성공", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "조회 성공", value = """
                            {"isSuccess":true,"message":"근무 인원 목록 조회에 성공했습니다.","details":{"date":"2026-07-15","workers":[{"userId":1,"name":"홍길동","studentId":"202211414","department":"컴퓨터공학과","grade":3,"phoneNumber":"010-1234-5678","weeklyWorkedMinutes":240,"weeklyLimitMinutes":780,"monthlyWorkedMinutes":960,"monthlyLimitMinutes":1620,"totalChangeRequestCount":9,"approvedChangeRequestCount":3,"totalAttendanceIssueCount":9,"lateCount":3}],"page":0,"size":10,"totalElements":21,"totalPages":3,"first":true,"last":false}}
                            """))),
            @ApiResponse(responseCode = "400", description = "잘못된 조회 조건", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"INVALID_REQUEST","message":"잘못된 조회 조건입니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"UNAUTHORIZED","message":"인증이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"ADMIN_ACCESS_DENIED","message":"근무 인원 조회 권한이 없습니다."}
                            """))),
            @ApiResponse(responseCode = "404", description = "월 근무 설정 없음", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"WORK_SCHEDULE_SETTING_NOT_FOUND","message":"해당 월의 근무 설정을 찾을 수 없습니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다."}
                            """)))
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getWorkers(
            @Parameter(description = "조회 기준일 (yyyy-MM-dd)", example = "2026-07-15", required = true) @RequestParam String date,
            @Parameter(description = "이름 검색어 (부분 일치)", example = "홍길동") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지당 인원 수", example = "10") @RequestParam(required = false, defaultValue = "10") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminWorkerPageResponse details = adminWorkerService.getWorkers(
                userDetails.getUser().getOrganizationId(), date, keyword, page, size);
        return ResponseEntity.ok(Response.of(true, "근무 인원 목록 조회에 성공했습니다.", details));
    }
}
