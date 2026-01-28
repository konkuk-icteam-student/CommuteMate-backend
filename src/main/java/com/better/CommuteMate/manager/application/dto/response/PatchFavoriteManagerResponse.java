package com.better.CommuteMate.manager.application.dto.response;

import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Manager 즐겨찾기 등록 응답 DTO")
public class PatchFavoriteManagerResponse extends ResponseDetail{
    @Schema(description = "즐겨찾기 등록 및 해제된 담당자 ID")
    private final Long managerId;

    @Schema(description = "즐겨찾기 등록 및 해제된 카테고리 ID")
    private final Long categoryId;

    @Schema(description = "변경된 즐겨찾기 상태")
    private final boolean favorite;


    public PatchFavoriteManagerResponse(ManagerCategory managerCatgory) {
        super();
        this.managerId = managerCatgory.getManager().getId();
        this.categoryId = managerCatgory.getCategory().getId();
        this.favorite = managerCatgory.isFavorite();
    }
}
