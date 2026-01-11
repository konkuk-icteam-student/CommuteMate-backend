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
/**
 * 사용자 관련 요청을 처리하는 컨트롤러 클래스입니다.
 * <p>
 * 사용자 마이페이지 정보 조회, 근무 시간 조회 등의 기능을 제공합니다.
 * </p>
 */
public class UserController {

    private final UserService userService; // 사용자 비즈니스 로직을 처리하는 서비스

    /**
     * 내 정보 조회 API
     * <p>
     * 현재 로그인한 사용자의 정보를 조회합니다.
     * </p>
     * @param userDetails 인증된 사용자 정보 (SecurityContext)
     * @return 사용자 정보가 담긴 응답 객체 (200 OK)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<Response> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 서비스 계층을 통해 사용자 ID로 상세 정보 조회
        UserInfoResponse response = userService.getUserInfo(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "내 정보 조회 성공", response));
    }

    /**
     * 주간 근무 시간 조회 API
     * <p>
     * 이번 주(월요일 ~ 일요일)의 총 근무 시간을 조회합니다.
     * </p>
     * @param userDetails 인증된 사용자 정보
     * @return 주간 총 근무 분(minutes)이 담긴 응답 객체 (200 OK)
     */
    @Operation(summary = "주간 근무 시간 조회", description = "이번 주의 총 근무 시간을 조회합니다.")
    @GetMapping("/me/work-time/weekly")
    public ResponseEntity<Response> getWeeklyWorkTime(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 서비스 계층을 통해 주간 근무 시간 계산 및 조회
        UserWorkTimeResponse response = userService.getWeeklyWorkTime(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "주간 근무 시간 조회 성공", response));
    }

    /**
     * 월간 근무 시간 조회 API
     * <p>
     * 이번 달(1일 ~ 말일)의 총 근무 시간을 조회합니다.
     * </p>
     * @param userDetails 인증된 사용자 정보
     * @return 월간 총 근무 분(minutes)이 담긴 응답 객체 (200 OK)
     */
    @Operation(summary = "월간 근무 시간 조회", description = "이번 달의 총 근무 시간을 조회합니다.")
    @GetMapping("/me/work-time/monthly")
    public ResponseEntity<Response> getMonthlyWorkTime(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 서비스 계층을 통해 월간 근무 시간 계산 및 조회
        UserWorkTimeResponse response = userService.getMonthlyWorkTime(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "월간 근무 시간 조회 성공", response));
    }
}
