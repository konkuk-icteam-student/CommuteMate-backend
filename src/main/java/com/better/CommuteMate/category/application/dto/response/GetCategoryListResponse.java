package com.better.CommuteMate.category.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Category 리스트 응답 DTO")
public class GetCategoryListResponse {

    @Schema(description = "분류 ID", example = "1")
    private final Long categoryId;

    @Schema(description = "분류 이름", example = "시스템")
    private final String categoryName;

    public GetCategoryListResponse(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

}
