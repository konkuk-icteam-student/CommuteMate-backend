package com.better.CommuteMate.organization.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "조직 목록 조회 응답 DTO")
public class GetOrganizationListWrapper extends ResponseDetail {

    @Schema(description = "조직 목록")
    private final List<GetOrganizationListResponse> organizations;

    public GetOrganizationListWrapper(List<GetOrganizationListResponse> organizations) {
        super();
        this.organizations = organizations;
    }
}
