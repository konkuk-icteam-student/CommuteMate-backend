package com.better.CommuteMate.subcategory.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PostSubCategoryUpdateNameResponse extends ResponseDetail {

    @Schema(description = "변경된 소분류 ID")
    private final Long subCategoryId;

    @Schema(description = "변경된 소분류명")
    private final String subCategoryName;

    public PostSubCategoryUpdateNameResponse(Long subCategoryId, String subCategoryName) {
        super();
        this.subCategoryId = subCategoryId;
        this.subCategoryName = subCategoryName;
    }
}