package com.better.CommuteMate.faq.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "FAQ 등록 응답 DTO")
@Getter
public class PostFaqResponse extends ResponseDetail {
    @Schema(description = "등록된 FAQ ID", example = "1")
    Long faqId;

    public PostFaqResponse(Long faqId) {
        super();
        this.faqId = faqId;
    }
}
