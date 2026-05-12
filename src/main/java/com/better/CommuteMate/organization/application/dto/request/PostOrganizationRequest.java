package com.better.CommuteMate.organization.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조직 등록 요청 DTO")
public record PostOrganizationRequest(

        @Schema(description = "등록할 조직명", example = "정보운영팀")
        String organizationName
) {}
