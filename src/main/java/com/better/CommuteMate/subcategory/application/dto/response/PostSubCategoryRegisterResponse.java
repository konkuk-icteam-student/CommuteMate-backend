package com.better.CommuteMate.subcategory.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "SubCategory 등록 응답 DTO")
public class PostSubCategoryRegisterResponse extends ResponseDetail {
    @Schema(description = "등록된 소분류 ID")
    private final Long subCategoryId;

    public PostSubCategoryRegisterResponse(Long subCategoryId) {
        super();
        this.subCategoryId = subCategoryId;
    }
}
