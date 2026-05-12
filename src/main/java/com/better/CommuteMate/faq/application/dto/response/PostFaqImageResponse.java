package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "FAQ 이미지 업로드 응답 DTO")
@Getter
public class PostFaqImageResponse extends ResponseDetail {
    @Schema(description = "업로드 된 이미지 Url", example = "https://kusd.konkuk.ac.kr/uploads/prod/faq/images/3f1c2a9b-1234-4abc-9def-123456789abc.png")
    private final String imageUrl;

    public PostFaqImageResponse(String imageUrl) {
        super();
        this.imageUrl = imageUrl;
    }
}