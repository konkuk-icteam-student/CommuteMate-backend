package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category 리스트 응답 DTO")
public class GetCategoryListResponse extends ResponseDetail {

    @Schema(description = "대분류 ID", example = "1")
    private final Long categoryId;

    @Schema(description = "대분류 이름", example = "시스템")
    private final String categoryName;

    public GetCategoryListResponse(Long categoryId, String categoryName) {
        super(); // timestamp 설정
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
