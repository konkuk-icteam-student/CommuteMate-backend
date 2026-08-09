package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.TodayScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈 화면", description = "홈 화면용 정보 조회 API")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeTodayController {

    private final HomeService homeService;

    @GetMapping("/today")
    @Operation(
            summary = "오늘 근무 일정 목록 조회",
            description = "오늘 날짜의 승인·신청 상태인 근무 일정 목록과 각 일정의 실시간 근무 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "오늘 근무 일정 조회 성공",
                                              "details": {
                                                "date": "2026-10-13",
                                                "schedules": [
                                                  {
                                                    "scheduleId": 1,
                                                    "label": "오전 근무",
                                                    "start": "09:00",
                                                    "end": "12:00",
                                                    "workStatusCode": "WK02",
                                                    "checkedIn": true,
                                                    "checkInTime": "2026-10-13T09:02:00"
                                                  },
                                                  {
                                                    "scheduleId": 2,
                                                    "label": "오후 근무",
                                                    "start": "13:00",
                                                    "end": "17:00",
                                                    "workStatusCode": "WK01",
                                                    "checkedIn": false,
                                                    "checkInTime": null
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getTodaySchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TodayScheduleResponse response = homeService.getTodaySchedules(userDetails.getUser().getUserId());
        return ResponseEntity.ok(Response.of(true, "오늘 근무 일정 조회 성공", response));
    }
}
