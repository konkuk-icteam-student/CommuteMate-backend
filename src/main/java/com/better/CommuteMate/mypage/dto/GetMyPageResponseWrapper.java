package com.better.CommuteMate.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 조회 응답")
public record GetMyPageResponseWrapper(

        @Schema(example = "true")
        boolean isSuccess,

        @Schema(example = "마이페이지 조회 성공")
        String message,

        GetMyPageResponse details
) {
}