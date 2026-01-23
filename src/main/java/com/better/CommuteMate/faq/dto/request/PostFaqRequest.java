package com.better.CommuteMate.faq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "FAQ 등록 요청 DTO")
public record PostFaqRequest(
        @NotBlank
        @Schema(description = "제목", example = "학정시 로그인 오류")
        String title,

        @Schema(description = "민원인 이름", example = "홍길동")
        String complainantName,

        @NotBlank
        @Schema(description = "답변", example = "핸드폰 앱에서 실행 안되고 ...")
        String answer,

        @Schema(description = "비고", example = "버튼 위치 변경됨")
        String etc,

        @NotNull
        @Schema(description = "분류 id", example = "1")
        Long categoryId,

        @Schema(description = "내용", example = "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸")
        String content

) {}