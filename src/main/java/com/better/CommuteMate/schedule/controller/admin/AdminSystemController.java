package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.AdminSystemService;
import com.better.CommuteMate.schedule.controller.admin.dtos.SystemCreatedYearResponse;
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

@Tag(name = "관리자 시스템", description = "관리자용 시스템 정보 조회 API")
@SecurityRequirement(name = "JWT")
@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    @GetMapping("/created-year")
    @Operation(
            summary = "시스템 생성 연도 조회",
            description = "로그인한 사용자가 소속된 조직의 생성 연도를 조회합니다. " +
                    "관리자 화면의 연도 선택 UI에서 선택 가능한 최소 연도(하한선)로 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "시스템 생성 연도 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "조회 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "시스템 생성 연도를 조회했습니다.",
                                              "details": {
                                                "createdYear": 2024
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    public ResponseEntity<Response> getCreatedYear(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SystemCreatedYearResponse details = adminSystemService.getCreatedYear(
                userDetails.getUser().getOrganizationId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "시스템 생성 연도를 조회했습니다.",
                details
        ));
    }
}
