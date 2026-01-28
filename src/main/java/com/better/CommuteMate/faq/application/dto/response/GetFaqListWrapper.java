package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "FAQ 목록 조회 응답 Wrapper")
@Getter
public class GetFaqListWrapper extends ResponseDetail {

    @Schema(description = "faq 목록")
    private final List<GetFaqListResponse> faqs;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "1")
    private final int page;

    @Schema(description = "전체 페이지 번호", example = "10")
    private final int totalPages;

    public GetFaqListWrapper(
            List<GetFaqListResponse> faqs,
            int page,
            int totalPages
    ) {
        super();
        this.faqs = faqs;
        this.page = page;
        this.totalPages = totalPages;
    }
}
