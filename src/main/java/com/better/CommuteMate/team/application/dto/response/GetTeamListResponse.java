package com.better.CommuteMate.team.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "소속 상세 정보 조회 응답 DTO")
public class GetTeamListResponse {

    @Schema(description = "소속 ID", example = "1")
    private final Long teamId;

    @Schema(description = "소속 이름", example = "정보운영팀")
    private final String teamName;

    public GetTeamListResponse(Long teamId, String teamName) {
        this.teamId = teamId;
        this.teamName = teamName;
    }

}
