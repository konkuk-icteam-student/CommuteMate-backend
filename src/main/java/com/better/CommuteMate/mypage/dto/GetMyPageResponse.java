package com.better.CommuteMate.mypage.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "마이페이지 조회 응답")
public class GetMyPageResponse extends ResponseDetail {

    @Schema(description = "사용자 이름", example = "김담당")
    private final String name;

    @Schema(description = "이메일", example = "1234@konkuk.ac.kr")
    private final String email;

    @Schema(description = "소속 조직 ID", example = "1")
    private final Long organizationId;

    @Schema(description = "소속 조직명", example = "정보운영팀")
    private final String organizationName;

    @Schema(description = "작성 완료 업무일지 개수", example = "24")
    private final long publishedCount;

    @Schema(description = "임시저장 업무일지 개수", example = "4")
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