package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.faq.entity.Faq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "내 업무일지 목록 항목 DTO")
public class GetMyFaqListResponse {

    @Schema(description = "FAQ ID", example = "1")
    private final Long faqId;

    @Schema(description = "업무일지 제목", example = "학정시 로그인 오류")
    private final String title;

    @Schema(description = "작성자 이름", example = "김담당")
    private final String writerName;

    @Schema(description = "소속 조직명", example = "정보운영팀")
    private final String organizationName;

    @Schema(description = "최종 수정일", example = "2026-07-16")
    private final LocalDate updatedDate;

    public GetMyFaqListResponse(
            Faq faq,
            String organizationName
    ) {
        this.faqId = faq.getId();
        this.title = faq.getTitle();
        this.writerName = faq.getWriter().getName();
        this.organizationName = organizationName;
        this.updatedDate = faq.getUpdatedDate();
    }
}