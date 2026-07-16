package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "마이페이지 조회 응답 DTO")
@Getter
public class GetMyPageResponse extends ResponseDetail {

    private final String name;
    private final String email;
    private final Long organizationId;
    private final String organizationName;
    private final long publishedCount;
    private final long draftCount;

    public GetMyPageResponse(
            String name,
            String email,
            Long organizationId,
            String organizationName,
            long publishedCount,
            long draftCount
    ) {
        super();
        this.name = name;
        this.email = email;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.publishedCount = publishedCount;
        this.draftCount = draftCount;
    }
}