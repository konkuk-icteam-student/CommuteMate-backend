package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category 등록 응답 DTO")
public class PostCategoryRegisterResponse extends ResponseDetail {

    @Schema(description = "등록된 대분류 ID", example = "1")
    private final Long categoryId;

    public PostCategoryRegisterResponse(Long categoryId) {
        super();
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}