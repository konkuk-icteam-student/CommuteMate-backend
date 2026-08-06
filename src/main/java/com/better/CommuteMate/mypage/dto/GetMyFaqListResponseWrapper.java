package com.better.CommuteMate.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 업무일지 목록 조회 전체 응답")
public record GetMyFaqListResponseWrapper(

        @Schema(description = "성공 여부", example = "true")
        boolean isSuccess,

        @Schema(
                description = "응답 메시지",
                example = "내가 작성한 업무일지 목록 조회 성공"
        )
        String message,

        @Schema(description = "내 업무일지 목록 상세 응답")
        GetMyFaqListWrapper details
) {
}