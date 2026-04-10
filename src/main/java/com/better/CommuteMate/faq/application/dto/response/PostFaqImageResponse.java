package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "FAQ 등록 응답 DTO")
@Getter
public class PostFaqImageResponse extends ResponseDetail {
    @Schema(description = "업로드 된 이미지 Url", example = "https://your-bucket.s3.ap-northeast-2.amazonaws.com/faq/uuid.png")
    String imageUrl;

    public PostFaqImageResponse(String imageUrl) {
        super();
        this.imageUrl = imageUrl;
    }
}