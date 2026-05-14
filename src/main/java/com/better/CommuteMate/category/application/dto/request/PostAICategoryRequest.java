package com.better.CommuteMate.category.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 카테고리 추천 요청 DTO")
public record PostAICategoryRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Schema(
                description = "FAQ 제목",
                example = "로그인이 안돼요",
                required = true
        )
        String title,

        @Schema(
                description = "FAQ 내용",
                example = "아이디 비밀번호 입력했는데 계속 실패합니다"
        )
        String content

) {}