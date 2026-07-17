package com.better.CommuteMate.faq.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "FAQ 임시저장 요청 DTO")
public record PostDraftFaqRequest(

        @Schema(description = "제목")
        String title,

        @Schema(description = "민원인 이름")
        String complainantName,

        @Schema(description = "내용")
        String content,

        @Schema(description = "답변")
        String answer,

        @Schema(description = "비고")
        String etc,

        @Schema(description = "카테고리 id 목록")
        List<Long> categoryIds
) {
}
