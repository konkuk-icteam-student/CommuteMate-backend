package com.better.CommuteMate.user.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.user.application.UserService;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 마이페이지", description = "사용자 정보 및 근무 시간 통계 API")
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<Response> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserInfoResponse response = userService.getUserInfo(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "내 정보 조회 성공", response));
    }

    @Operation(summary = "주간 근무 시간 조회", description = "이번 주의 총 근무 시간을 조회합니다.")
    @GetMapping("/me/work-time/weekly")
    public ResponseEntity<Response> getWeeklyWorkTime(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserWorkTimeResponse response = userService.getWeeklyWorkTime(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "주간 근무 시간 조회 성공", response));
    }

    @Operation(summary = "월간 근무 시간 조회", description = "이번 달의 총 근무 시간을 조회합니다.")
    @GetMapping("/me/work-time/monthly")
    public ResponseEntity<Response> getMonthlyWorkTime(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserWorkTimeResponse response = userService.getMonthlyWorkTime(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "월간 근무 시간 조회 성공", response));
    }
}
