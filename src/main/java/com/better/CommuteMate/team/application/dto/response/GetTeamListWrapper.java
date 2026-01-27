package com.better.CommuteMate.team.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "소속 목록 조회 응답 DTO")
public class GetTeamListWrapper extends ResponseDetail {

    @Schema(description = "소속 목록")
    private final List<GetTeamListResponse> teams;

    public GetTeamListWrapper(List<GetTeamListResponse> teams) {
        super();
        this.teams = teams;
    }
}
