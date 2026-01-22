package com.better.CommuteMate.manager.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "담당자 등록 응답 DTO")
public class PostManagerResponse extends ResponseDetail {

    @Schema(description = "등록된 담당자 ID", example = "1")
    private final Long managerId;

    @Schema(description = "등록된 담당자 카테고리 ID", example = "1")
    private final Long categoryId;

    public PostManagerResponse(Long managerId, Long categoryId) {
        super();
        this.managerId = managerId;
        this.categoryId = categoryId;
    }
}