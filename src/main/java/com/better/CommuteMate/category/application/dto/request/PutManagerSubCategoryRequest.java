package com.better.CommuteMate.category.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Manager-SubCategory 매핑 수정 요청 DTO")
public record PutManagerSubCategoryRequest(

        @Schema(description = "매니저 ID", example = "5")
        Integer managerId,

        @Schema(description = "새로 매핑할 subCategory 이름 리스트", example = "[\"도서관시스템\", \"학사정보시스템\"]")
        List<String> subCategoryNames
) {}
