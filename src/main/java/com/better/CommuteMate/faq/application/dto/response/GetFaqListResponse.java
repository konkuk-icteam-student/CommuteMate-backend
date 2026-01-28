package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.domain.faq.entity.Faq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "FAQ 목록 조회 응답 DTO")
@Getter
public class GetFaqListResponse {

    @Schema(description = "faq id", example = "1")
    private final Long faqId;

    @Schema(description = "faq 제목", example = "학정시 로그인 오륲")
    private final String title;

    @Schema(description = "faq 작성 날짜 (수정된 경우 최근 수정 날짜)", example = "2025-01-25T13:20:00")
    private final LocalDateTime createdAt;

    @Schema(description = "faq 삭제 여부", example = "true")
    private final Boolean deletedFlag;

    public GetFaqListResponse(Faq faq) {
        this.faqId = faq.getId();
        this.title = faq.getTitle();
        this.createdAt = faq.getCreatedAt();
        this.deletedFlag = faq.getDeletedFlag();
    }
}
