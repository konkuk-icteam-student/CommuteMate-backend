package com.better.CommuteMate.subcategory.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SubCategory 등록 요청 DTO")
public record PostSubCategoryRegisterRequest(

        @Schema(description = "등록할 소분류명", example = "도서관")
        String subCategoryName,

        @Schema(description = "소속될 대분류 id", example = "1")
        Long categoryId
) {}