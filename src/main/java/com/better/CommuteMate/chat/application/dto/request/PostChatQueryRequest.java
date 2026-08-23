package com.better.CommuteMate.chat.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "챗봇 질의 요청 DTO")
public record PostChatQueryRequest(

        @NotBlank(message = "질문 내용은 필수입니다.")
        @Size(max = 2000, message = "질문은 2000자를 초과할 수 없습니다.")
        @Schema(
                description = "규정/FAQ에 대해 질문할 내용",
                example = "연차는 며칠까지 이월할 수 있나요?",
                required = true
        )
        String query

) {}
