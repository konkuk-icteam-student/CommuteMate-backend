package com.better.CommuteMate.mypage.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.mypage.application.MyPageService;
import com.better.CommuteMate.mypage.dto.MyPageInfoResponse;
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

@Tag(name = "마이페이지", description = "마이페이지 정보 조회 API")
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageV1Controller {

    private final MyPageService myPageService;

    @GetMapping
    @Operation(
            summary = "마이페이지 조회",
            description = """
                    인증된 사용자의 프로필 정보와 이번 주 및 이번 달 근무 시간 현황을 조회합니다.
                    - roleName은 코드 표시값(학생/관리자)을 그대로 반환합니다.
                    - UserProfile이 미등록된 경우 department와 studentId는 null로 응답됩니다.
                    - week.workedHours / month.workedHours는 실제 근무가 완료된 시간입니다 (CT01 출근 기록 존재 + 슬롯 종료 시각 경과 기준).
                    - week.limitHours는 monthly_setting의 weekly_max_minutes를 시간 환산한 값이며, 설정이 없으면 0입니다.
                    - month.limitHours는 monthly_required_minutes를 시간 환산한 값이며, 설정이 없으면 0입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "마이페이지 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "마이페이지 정보를 조회했습니다.",
                                              "details": {
                                                "userName": "홍길동",
                                                "roleName": "학생",
                                                "organizationName": "건국대학교 정보운영팀",
                                                "department": "컴퓨터공학과",
                                                "studentId": "202412345",
                                                "week": {
                                                  "workedHours": 3,
                                                  "limitHours": 13
                                                },
                                                "month": {
                                                  "workedHours": 13,
                                                  "limitHours": 27
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMyPageInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        Long organizationId = userDetails.getUser().getOrganizationId();
        MyPageInfoResponse response = myPageService.getMyPageInfo(userId, organizationId);
        return ResponseEntity.ok(Response.of(true, "마이페이지 정보를 조회했습니다.", response));
    }
}
