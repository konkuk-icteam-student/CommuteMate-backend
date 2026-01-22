package com.better.CommuteMate.manager.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "manager 등록 요청 DTO")
public record PostManagerRequest(
        @Schema(description = "담당자 이름", example = "홍길동")
        String name,

        @Schema(description = "소속", example = "정보운영팀")
        String team,

        @Schema(description = "카테고리 id", example = "1")
        Long categoryId,

        @Schema(description = "번호", example = "01012345678")
        String phonenum
) {}
