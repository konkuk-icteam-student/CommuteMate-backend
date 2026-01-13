package com.better.CommuteMate.faq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FAQ 등록 요청 DTO")
public record PostFaqCreateRequest(
        @Schema(description = "작성자 ID", example = "1")
        int userId, // Todo 인증 로직 추가되면 삭제
        @Schema(description = "분류명", example = "인사관리")
        String category,
        @Schema(description = "제목", example = "학정시 로그인 오류")
        String title,
        @Schema(description = "내용", example = "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸")
        String content,
        @Schema(description = "첨부파일", example = "test.pdf")
        String attachmentUrl,
        @Schema(description = "비고", example = "없음")
        String etc
) {}