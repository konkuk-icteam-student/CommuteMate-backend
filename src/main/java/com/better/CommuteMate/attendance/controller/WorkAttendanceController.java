package com.better.CommuteMate.attendance.controller;

import com.better.CommuteMate.attendance.application.WorkAttendanceService;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryListResponse;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.attendance.controller.dto.CheckInRequest;
import com.better.CommuteMate.attendance.controller.dto.CheckOutRequest;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "출퇴근 인증", description = "QR 출퇴근 인증 및 이력 조회 API")
@RestController
@RequestMapping("api/v1/attendance")
@RequiredArgsConstructor
public class WorkAttendanceController {

    private final WorkAttendanceService workAttendanceService;

    @Operation(summary = "관리자: QR 인증 토큰 발급", description = "관리자 태블릿에서 사용할 QR 인증 토큰을 발급합니다. 60초간 유효합니다.")
    @GetMapping("/qr-token")
    public ResponseEntity<Response> getQrToken() {
        QrTokenResponse response = workAttendanceService.generateQrToken();
        return ResponseEntity.ok(Response.of(true, "QR 토큰 발급 성공", response));
    }

    @Operation(summary = "사용자: 출근 체크", description = "QR 토큰을 사용하여 출근을 인증합니다.")
    @PostMapping("/check-in")
    public ResponseEntity<Response> checkIn(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CheckInRequest request) {
        workAttendanceService.checkIn(userDetails.getUser().getUserId(), request.getQrToken());
        return ResponseEntity.ok(Response.of(true, "출근 처리가 완료되었습니다.", null));
    }

    @Operation(summary = "사용자: 퇴근 체크", description = "퇴근을 인증합니다. (QR 불필요)")
    @PostMapping("/check-out")
    public ResponseEntity<Response> checkOut(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) CheckOutRequest request) {
        workAttendanceService.checkOut(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "퇴근 처리가 완료되었습니다.", null));
    }

    @Operation(summary = "오늘의 출퇴근 기록 조회", description = "오늘 날짜의 출퇴근 이력을 조회합니다.")
    @GetMapping("/today")
    public ResponseEntity<Response> getTodayAttendance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AttendanceHistoryResponse> history = workAttendanceService.getAttendanceHistory(
                userDetails.getUser().getUserId(), LocalDate.now());
        return ResponseEntity.ok(Response.of(true, "오늘의 출퇴근 기록 조회 성공", 
                AttendanceHistoryListResponse.of(history)));
    }

    @Operation(summary = "출퇴근 이력 조회", description = "특정 날짜의 출퇴근 이력을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<Response> getAttendanceHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceHistoryResponse> history = workAttendanceService.getAttendanceHistory(
                userDetails.getUser().getUserId(), date);
        return ResponseEntity.ok(Response.of(true, "출퇴근 이력 조회 성공", 
                AttendanceHistoryListResponse.of(history)));
    }
}
