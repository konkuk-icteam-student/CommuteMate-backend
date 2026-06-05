package com.better.CommuteMate.organization.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "조직 상세 정보 조회 응답 DTO")
public class GetOrganizationListResponse {

    @Schema(description = "조직 ID", example = "1")
    private final Long organizationId;

    @Schema(description = "조직 이름", example = "정보운영팀")
    private final String organizationName;

    public GetOrganizationListResponse(Long organizationId, String organizationName) {
        this.organizationId = organizationId;
        this.organizationName = organizationName;
    }

}
