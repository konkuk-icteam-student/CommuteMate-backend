package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "FAQ 파일 업로드 응답 DTO")
@Getter
public class PostFaqFileResponse extends ResponseDetail {
    @Schema(description = "업로드 된 파일 Url", example = "https://kusd.konkuk.ac.kr/uploads/prod/faq/files/3f1c2a9b-1234-4abc-9def-123456789abc.pdf")
    private final String fileUrl;

    @Schema(description = "원본 파일 이름", example = "강의자료.pdf")
    private final String originalName;

    public PostFaqFileResponse(String fileUrl, String originalName) {
        super();
        this.fileUrl = fileUrl;
        this.originalName = originalName;
    }
}