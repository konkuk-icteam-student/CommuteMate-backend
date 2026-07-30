package com.better.CommuteMate.mypage.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.mypage.application.MyPageService;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {
    private final MyPageService myPageService;

    @Operation(
            summary = "마이페이지 조회",
            description = "사용자 프로필과 작성 완료 및 임시저장 업무일지 개수를 조회하는 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 조직을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                new Response(
                        true,
                        "마이페이지 조회 성공",
                        myPageService.getMyPage(userDetails.getUserId())
                )
        );
    }

    @Operation(
            summary = "내가 작성한 업무일지 목록 조회",
            description = "작성 완료(PUBLISHED) 상태의 업무일지 목록을 조회하는 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업무일지 목록 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/faqs")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMyPublishedFaqs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(
                new Response(
                        true,
                        "내가 작성한 업무일지 목록 조회 성공",
                        myPageService.getMyPublishedFaqs(
                                userDetails.getUserId(),
                                page
                        )
                )
        );
    }

    @Operation(
            summary = "임시저장 업무일지 목록 조회",
            description = "임시저장(DRAFT) 상태의 업무일지 목록을 조회하는 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "임시저장 업무일지 목록 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/faqs/drafts")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> getMyDraftFaqs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(
                new Response(
                        true,
                        "임시저장 업무일지 목록 조회 성공",
                        myPageService.getMyDraftFaqs(
                                userDetails.getUserId(),
                                page
                        )
                )
        );
    }
}
