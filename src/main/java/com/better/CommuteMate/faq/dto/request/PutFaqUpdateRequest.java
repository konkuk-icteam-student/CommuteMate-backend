package com.better.CommuteMate.faq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FAQ 수정 요청 DTO")
public record PutFaqUpdateRequest(
        @Schema(description = "수정자 ID", example = "1")
        int userId,
        @Schema(description = "제목", example = "인사관리")
        String title,
        @Schema(description = "내용", example = "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸")
        String content,
        @Schema(description = "비고", example = "없음")
        String etc,
        @Schema(description = "첨부파일", example = "test.pdf")
        String attachmentUrl,
        @Schema(description = "담당자 이름", example = "김철수")
        String manager
) {}