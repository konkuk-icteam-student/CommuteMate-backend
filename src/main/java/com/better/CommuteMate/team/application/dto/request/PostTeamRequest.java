package com.better.CommuteMate.team.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소속 등록 요청 DTO")
public record PostTeamRequest(

        @Schema(description = "등록할 소속명", example = "정보운영팀")
        String teamName
) {}
