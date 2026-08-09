package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.HomeCheckInRequest;
import com.better.CommuteMate.home.controller.dto.HomeCheckInResponse;
import com.better.CommuteMate.home.controller.dto.TodayScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            summary = "오늘 근무 일정 조회",
            description = """
                    오늘 날짜의 근무 일정(신청·승인 상태)을 조회합니다.
                    연속된 슬롯은 하나의 근무 카드로 병합되며, 병합에 포함된 슬롯 ID 목록(scheduleIds)을 함께 반환합니다.
                    workStatusCode는 현재 시각 기준 실시간으로 계산됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공 (2개 근무 카드)",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "오늘의 근무 현황을 조회했습니다.",
                                              "details": {
                                                "date": "2026-10-13",
                                                "schedules": [
                                                  {
                                                    "scheduleIds": [1, 2, 3, 4, 5],
                                                    "label": "오전 근무",
                                                    "start": "09:00",
                                                    "end": "11:30",
                                                    "workStatusCode": "WK02",
                                                    "checkedIn": true,
                                                    "checkInTime": "2026-10-13T09:02:00"
                                                  },
                                                  {
                                                    "scheduleIds": [6, 7, 8, 9],
                                                    "label": "오후 근무",
                                                    "start": "13:30",
                                                    "end": "15:30",
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
        return ResponseEntity.ok(Response.of(true, "오늘의 근무 현황을 조회했습니다.", response));
    }

    @PostMapping("/check-in")
    @Operation(
            summary = "홈 출근 처리",
            description = """
                    조회 API(`GET /api/v1/home/today`)에서 받은 병합 근무의 scheduleIds를 그대로 전달하면,
                    해당 슬롯 전체에 출근 기록을 한 번에 남깁니다.
                    - 출근 가능 시간: 첫 슬롯 start 기준 + 10분
                    - 이미 출근되었거나 시간이 지난 경우 409 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "출근 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "출근 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "출근 처리되었습니다.",
                                              "details": {
                                                "scheduleIds": [1, 2, 3, 4, 5],
                                                "checkInTime": "2026-10-13T09:02:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 오류 또는 연속되지 않은 슬롯",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "scheduleIds 누락",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "출근할 근무 일정을 선택해야 합니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "연속되지 않은 슬롯",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "연속된 근무 일정이 아닙니다.",
                                                      "details": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "일정 없음 또는 유효하지 않은 일정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "일정 없음",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "message": "근무 일정을 찾을 수 없습니다.",
                                              "details": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 출근됨 또는 출근 가능 시간 초과",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이미 출근 처리됨",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "이미 출근 처리된 근무입니다.",
                                                      "details": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "출근 가능 시간 초과",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "message": "출근 가능 시간이 지나 결근 처리되었습니다.",
                                                      "details": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> checkIn(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HomeCheckInRequest request) {
        HomeCheckInResponse response = homeService.checkIn(
                userDetails.getUser().getUserId(), request.getScheduleIds());
        return ResponseEntity.ok(Response.of(true, "출근 처리되었습니다.", response));
    }
}
