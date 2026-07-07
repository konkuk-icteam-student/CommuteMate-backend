package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.category.application.dto.CategoryDto;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "AI 카테고리 추천 응답 DTO")
public class PostAICategoryResponse extends ResponseDetail {

    @Schema(description = "추천된 카테고리 목록")
    private final List<CategoryDto> categories;

    public PostAICategoryResponse(List<CategoryDto> categories) {
        super();
        this.categories = categories;
    }
}