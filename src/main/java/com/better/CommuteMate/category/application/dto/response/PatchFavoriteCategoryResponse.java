package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Category 즐겨찾기 등록 응답 DTO")
public class PatchFavoriteCategoryResponse extends ResponseDetail{
    @Schema(description = "즐겨찾기 등록 및 해제된 분류 ID")
    private final Long categoryId;

    @Schema(description = "변경된 즐겨찾기 상태")
    private final boolean favorite;


    public PatchFavoriteCategoryResponse(Long categoryId, boolean favorite) {
        super();
        this.categoryId = categoryId;
        this.favorite = favorite;
    }
}
