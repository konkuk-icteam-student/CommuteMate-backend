package com.better.CommuteMate.mypage.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "내 업무일지 목록 조회 응답 DTO")
public class GetMyFaqListWrapper extends ResponseDetail {

    @Schema(description = "업무일지 목록")
    private final List<GetMyFaqListResponse> faqs;

    @Schema(description = "현재 페이지", example = "0")
    private final int currentPage;

    @Schema(description = "전체 페이지 수", example = "3")
    private final int totalPages;

    @Schema(description = "전체 업무일지 수", example = "24")
    private final long totalElements;

    public GetMyFaqListWrapper(
            List<GetMyFaqListResponse> faqs,
            int currentPage,
            int totalPages,
            long totalElements
    ) {
        super();
        this.faqs = faqs;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }
}