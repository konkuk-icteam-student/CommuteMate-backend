package com.better.CommuteMate.category.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리 수정 요청 DTO")
public record PutCategoryUpdateRequest(
        @Schema(description = "변경할 대분류 이름", example = "학생복지")
        String categoryName
) {}
