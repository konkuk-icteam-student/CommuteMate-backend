package com.better.CommuteMate.category.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리 수정 응답 DTO")
public record PutCategoryUpdateResponse(
        @Schema(description = "수정된 category id", example = "1")
        Long categoryId,

        @Schema(description = "변경된 category name", example = "학생복지")
        String updatedName
) {}
