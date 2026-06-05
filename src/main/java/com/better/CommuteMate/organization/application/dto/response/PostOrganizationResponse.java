package com.better.CommuteMate.organization.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "조직 등록 응답 DTO")
public class PostOrganizationResponse extends ResponseDetail {

    @Schema(description = "등록된 조직 ID", example = "1")
    private final Long organizationId;

    public PostOrganizationResponse(Long organizationId) {
        super();
        this.organizationId = organizationId;
    }
}