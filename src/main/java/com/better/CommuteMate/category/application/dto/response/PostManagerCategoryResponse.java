package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "manager-Category 매핑 등록 응답 DTO")
public class PostManagerCategoryResponse extends ResponseDetail {

    @Schema(description = "등록된 매핑 개수", example = "3")
    private final int count;

    public PostManagerCategoryResponse(int count) {
        super(); // timestamp 자동 설정
        this.count = count;
    }

}
