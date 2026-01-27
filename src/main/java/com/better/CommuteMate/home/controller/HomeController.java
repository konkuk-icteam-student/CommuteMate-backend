package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import com.better.CommuteMate.home.controller.dto.WeeklyWorkSummaryResponse;
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

    /**
     * 이번 주 및 이번 달 근무 시간 요약을 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return {@link WeeklyWorkSummaryResponse}를 포함한 API 응답
     */
    @Operation(
        summary = "주간/월간 근무 시간 요약 조회",
        description = "이번 주 전체 근무 시간, 이번 주 완료 근무 시간, 이번 달 완료 근무 시간을 조회합니다. " +
                      "완료 여부는 퇴근 체크 기록이 있는지로 판단합니다. " +
                      "시간은 0.5 단위(30분)로 표시됩니다."
    )
    @GetMapping("/work-summary")
    public ResponseEntity<Response> getWorkSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        WeeklyWorkSummaryResponse response = homeService.getWorkSummary(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "근무 시간 요약 조회 성공", response));
    }
}
