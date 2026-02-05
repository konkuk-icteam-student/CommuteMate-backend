package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "Category 목록 조회 응답 DTO")
public class GetCategoryListWrapper extends ResponseDetail {

    @Schema(description = "카테고리 목록")
    private final List<GetCategoryListResponse> categories;

    public GetCategoryListWrapper(List<GetCategoryListResponse> categories) {
        super();
        this.categories = categories;
    }
}
