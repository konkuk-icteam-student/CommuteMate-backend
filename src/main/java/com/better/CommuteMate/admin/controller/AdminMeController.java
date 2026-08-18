package com.better.CommuteMate.admin.controller;

import com.better.CommuteMate.admin.application.AdminMeService;
import com.better.CommuteMate.admin.controller.dto.AdminMeResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 정보", description = "로그인한 관리자 본인 정보 조회 API")
public class AdminMeController {

    private final AdminMeService adminMeService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('RL02')")
    @Operation(
            summary = "관리자 헤더 정보 조회",
            description = "로그인한 관리자의 이름과 소속 조직명을 조회합니다. 관리자 화면 헤더 표시에 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관리자 정보 조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = SUCCESS_EXAMPLE))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"message":"인증이 필요합니다.","details":null}
                            """))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess":false,"message":"해당 작업을 수행할 권한이 없습니다.","details":null}
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "관리자 정보 또는 소속 조직을 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "관리자 없음", value = """
                                    {"isSuccess":false,"message":"사용자를 찾을 수 없습니다. 다시 확인해주세요","details":null}
                                    """),
                            @ExampleObject(name = "소속 조직 없음", value = """
                                    {"isSuccess":false,"message":"존재하지 않는 organizationId입니다.","details":null}
                                    """)
                    })
            )
    })

    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminMeResponse details = adminMeService.getMe(userDetails.getUserId());
        return ResponseEntity.ok(Response.of(true, "관리자 정보 조회에 성공했습니다.", details));
    }

    private static final String SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "관리자 정보 조회에 성공했습니다.",
              "details": {
                "userId": 1,
                "adminName": "김송은",
                "teamName": "정보운영팀"
              }
            }
            """;
}
