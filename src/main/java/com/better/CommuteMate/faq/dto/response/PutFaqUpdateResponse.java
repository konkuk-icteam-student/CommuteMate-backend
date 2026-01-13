package com.better.CommuteMate.faq.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "FAQ 수정 응답 DTO")
public class PutFaqUpdateResponse extends ResponseDetail {
    @Schema(description = "수정된 FAQ ID", example = "1")
    Long faqId;

    public PutFaqUpdateResponse(Long faqId) {
        super();
        this.faqId = faqId;
    }
}

