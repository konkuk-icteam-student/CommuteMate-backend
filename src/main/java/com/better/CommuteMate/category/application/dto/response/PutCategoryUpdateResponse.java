package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "카테고리 수정 응답 DTO")
public class PutCategoryUpdateResponse extends ResponseDetail {

    @Schema(description = "수정된 category id", example = "1")
    private final Long categoryId;

    @Schema(description = "변경된 category name", example = "학생복지")
    private final String updatedName;

    public PutCategoryUpdateResponse(Long categoryId, String updatedName) {
        super();
        this.categoryId = categoryId;
        this.updatedName = updatedName;
    }

}