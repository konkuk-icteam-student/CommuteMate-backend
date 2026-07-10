package com.better.CommuteMate.faq.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "FAQ 임시저장 수정 요청 DTO")
public record PutDraftFaqUpdateRequest(

        @Schema(description = "제목", example = "로그인 오류 문의")
        String title,

        @Schema(description = "민원인 이름", example = "홍길동")
        String complainantName,

        @Schema(description = "질문 내용", example = "<p>질문 내용입니다.</p>")
        String content,

        @Schema(description = "답변 내용", example = "<p>답변 내용입니다.</p>")
        String answer,

        @Schema(description = "비고", example = "추가 메모")
        String etc,

        @Schema(description = "카테고리 ID 목록", example = "[1, 2]")
        List<Long> categoryIds
) {
}
