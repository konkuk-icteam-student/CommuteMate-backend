package com.better.CommuteMate.faq.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "FAQ 수정 요청 DTO")
public record PutFaqUpdateRequest(
        @NotBlank
        @Schema(description = "제목", example = "인사관리")
        String title,

        @Schema(description = "민원인 이름", example = "홍길동")
        String complainantName,

        @NotBlank
        @Schema(description = "답변", example = "핸드폰 앱에서 실행 안되고 ...")
        String answer,

        @Schema(description = "비고", example = "없음")
        String etc,

        @NotNull
        @Schema(description = "분류 id", example = "1")
        Long categoryId,

        @Schema(description = "내용", example = "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸")
        String content
) {}