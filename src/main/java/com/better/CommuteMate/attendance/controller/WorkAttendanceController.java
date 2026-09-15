package com.better.CommuteMate.attendance.controller;

import com.better.CommuteMate.attendance.application.WorkAttendanceService;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryListResponse;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "출퇴근 이력", description = "출퇴근 이력 조회 API")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class WorkAttendanceController {

    private final WorkAttendanceService workAttendanceService;

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
