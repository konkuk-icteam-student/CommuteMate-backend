package com.better.CommuteMate.user.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.user.application.AdminUserSearchService;
import com.better.CommuteMate.user.controller.dto.AdminUserSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 사용자 관리", description = "관리자용 조직 사용자 조회 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserSearchService searchService;

    @GetMapping("/search")
    @Operation(
            summary = "학생 사용자 검색",
            description = "로그인한 관리자와 같은 조직에 소속된 학생을 이름 부분 일치로 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "검색 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "message": "요청에 성공하였습니다.",
                                              "details": {
                                                "users": [
                                                  {"userId": "2", "userName": "박영희"},
                                                  {"userId": "3", "userName": "박지민"},
                                                  {"userId": "4", "userName": "박보검"}
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어 누락 또는 빈 문자열",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "검색어 누락",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "message": "검색어를 입력해주세요.",
                                              "details": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> searchUsers(
            @Parameter(
                    description = "학생 이름 검색어(최소 1자, 부분 일치)",
                    example = "박",
                    required = true
            )
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AdminUserSearchResponse details = searchService.search(
                userDetails.getUser().getOrganizationId(),
                keyword
        );
        return ResponseEntity.ok(Response.of(
                true, "요청에 성공하였습니다.", details
        ));
    }
}
