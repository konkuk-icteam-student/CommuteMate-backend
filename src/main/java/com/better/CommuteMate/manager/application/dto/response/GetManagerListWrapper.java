package com.better.CommuteMate.manager.application.dto.response;


import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "담당자 목록 조회 응답 DTO")
public class GetManagerListWrapper extends ResponseDetail {

    @Schema(description = "담당자 목록")
    private final List<GetManagerListResponse> managers;

    public GetManagerListWrapper(List<GetManagerListResponse> managers) {
        super();
        this.managers = managers;
    }

}

