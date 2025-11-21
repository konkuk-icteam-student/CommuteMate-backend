package com.better.CommuteMate.subcategory.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SubCategory 이름 수정 요청 DTO")
public record PostSubCategoryUpdateNameRequest(
        @Schema(description = "수정할 소분류 이름", example = "학사정보시스템")
        String subCategoryName
) {
}
