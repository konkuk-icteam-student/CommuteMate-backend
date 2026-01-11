package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈 화면", description = "홈 화면용 정보 조회 API")
@RestController
@RequestMapping("api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 오늘의 근무 시간 및 스케줄 요약 정보를 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return {@link HomeWorkTimeResponse}를 포함한 API 응답
     */
    @Operation(summary = "오늘의 근무 시간 조회", description = "오늘 누적된 근무 시간과 예정된 스케줄 개수를 조회합니다.")
    @GetMapping("/work-time")
    public ResponseEntity<Response> getTodayWorkTime(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        HomeWorkTimeResponse response = homeService.getTodayWorkTime(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "오늘의 근무 시간 조회 성공", response));
    }

    /**
     * 현재 사용자의 출퇴근 버튼 상태를 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return {@link HomeAttendanceStatusResponse}를 포함한 API 응답
     */
    @Operation(summary = "출퇴근 상태 조회", description = "현재 시간에 따른 출퇴근 버튼 상태를 조회합니다.")
    @GetMapping("/attendance-status")
    public ResponseEntity<Response> getAttendanceStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        HomeAttendanceStatusResponse response = homeService.getAttendanceStatus(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "출퇴근 상태 조회 성공", response));
    }
}
