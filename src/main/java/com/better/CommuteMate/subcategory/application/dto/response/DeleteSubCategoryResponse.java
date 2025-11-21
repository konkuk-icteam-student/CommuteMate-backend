package com.better.CommuteMate.subcategory.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "SubCategory 삭제 응답 DTO")
public class DeleteSubCategoryResponse extends ResponseDetail {

    @Schema(description = "삭제된 소분류 ID")
    private final Long subCategoryId;

    public DeleteSubCategoryResponse(Long subCategoryId) {
        super();
        this.subCategoryId = subCategoryId;
    }
}