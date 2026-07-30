package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.faq.entity.Faq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "내 업무일지 목록 항목 DTO")
public class GetMyFaqListResponse {

    private final Long faqId;
    private final String title;
    private final LocalDate updatedDate;

    public GetMyFaqListResponse(Faq faq) {
        this.faqId = faq.getId();
        this.title = faq.getTitle();
        this.updatedDate = faq.getUpdatedDate();
    }
}