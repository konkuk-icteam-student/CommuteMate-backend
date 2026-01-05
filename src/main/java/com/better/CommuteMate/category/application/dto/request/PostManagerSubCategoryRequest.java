package com.better.CommuteMate.category.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "manager-subCategory 매핑 등록 요청 DTO")
public record PostManagerSubCategoryRequest(
        @Schema(description = "매핑할 관리자 ID", example = "3")
        Integer managerId,

        @Schema(description = "연결할 subCategory ID 리스트", example = "[1, 2, 3]")
        List<Integer> subCategoryIds
) {}
