package com.better.CommuteMate.faq.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "FAQ AI 검색 요청")
public record GetFaqAISearchRequest(
        @Schema(description = "소속 ID", example = "1", nullable = true)
        Long organizationId,
        @Schema(description = "카테고리 ID", example = "1", nullable = true)
        Long categoryId,
        @Schema(description = "검색 키워드", example = "로그인", nullable = true)
        String keyword,
        @Schema(description = "검색 시작 날짜", example = "2026-05-01", nullable = true)
        LocalDate startDate,
        @Schema(description = "검색 종료 날짜", example = "2026-05-31", nullable = true)
        LocalDate endDate,
        @Schema(description = "페이지 번호(0부터 시작)", example = "0", defaultValue = "0")
        Integer page
) { }
