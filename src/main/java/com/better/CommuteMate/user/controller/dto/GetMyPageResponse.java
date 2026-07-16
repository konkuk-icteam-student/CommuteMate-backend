package com.better.CommuteMate.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 조회 응답 DTO")
public record GetMyPageResponse(

        @Schema(description = "사용자 이름", example = "김담당")
        String name,

        @Schema(description = "이메일", example = "1234@konkuk.ac.kr")
        String email,

        @Schema(description = "소속 조직 ID", example = "1")
        Long organizationId,

        @Schema(description = "소속 조직명", example = "정보운영팀")
        String organizationName,

        @Schema(description = "작성 완료 업무일지 개수", example = "24")
        long publishedCount,

        @Schema(description = "임시저장 업무일지 개수", example = "4")
        long draftCount
) {
}
