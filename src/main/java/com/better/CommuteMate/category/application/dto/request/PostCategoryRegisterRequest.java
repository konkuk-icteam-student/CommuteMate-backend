package com.better.CommuteMate.category.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category 등록 요청 DTO")
public record PostCategoryRegisterRequest(

        @Schema(description = "등록할 분류명", example = "인사관리")
        String categoryName
) {}
