package com.better.CommuteMate.category.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category 등록 응답 DTO")
public record PostCategoryRegisterResponse(

        @Schema(description = "등록된 대분류 ID", example = "1")
        Long categoryId
) {}
