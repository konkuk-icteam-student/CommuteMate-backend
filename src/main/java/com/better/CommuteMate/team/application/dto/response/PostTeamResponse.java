package com.better.CommuteMate.team.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "소속 등록 응답 DTO")
public class PostTeamResponse extends ResponseDetail {

    @Schema(description = "등록된 소속 ID", example = "1")
    private final Long teamId;

    public PostTeamResponse(Long teamId) {
        super();
        this.teamId = teamId;
    }
}