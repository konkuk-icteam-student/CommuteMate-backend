package com.better.CommuteMate.subcategory.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "SubCategory 즐겨찾기 등록 응답 DTO")
public class PatchFavoriteSubCategoryResponse extends ResponseDetail{
    @Schema(description = "즐겨찾기 등록 및 해제된 소분류 ID")
    private final Long subCategoryId;

    @Schema(description = "변경된 즐겨찾기 상태")
    private final boolean favorite;


    public PatchFavoriteSubCategoryResponse(Long subCategoryId, boolean favorite) {
        super();
        this.subCategoryId = subCategoryId;
        this.favorite = favorite;
    }
}
