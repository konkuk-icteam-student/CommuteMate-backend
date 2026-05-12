package com.better.CommuteMate.faq.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;


@Schema(description = "FAQ 등록 요청 DTO")
public record PostFaqRequest(

        @NotBlank(message = "제목은 필수 입력값입니다.")
        @Schema(description = "제목", example = "학정시 로그인 오류")
        String title,

        @Schema(description = "민원인 이름", example = "홍길동")
        String complainantName,

        @NotBlank(message = "답변은 필수 입력값입니다.")
        @Schema(description = "답변 (HTML)", example = "<p>앱 재등록 후 다시 로그인해주세요.</p><img src=\"https://example.com/uploads/faq/images/sample-image.png\">")
        String answer,

        @Schema(description = "비고", example = "버튼 위치 변경됨")
        String etc,

        @NotEmpty(message = "카테고리는 최소 1개 이상 선택해야 합니다.")
        @Size(max = 3, message = "카테고리는 최대 3개까지 가능합니다.")
        @Schema(description = "분류 id 목록 (최대 3개)", example = "[1, 2, 3]")
        List<Long> categoryIds,

        @Schema(description = "내용 (HTML)", example = "<p>학정시 로그인 중 OTP 오류가 발생합니다.</p><img src=\"https://example.com/uploads/faq/images/sample-image.png\">")
        String content,

        @Schema(description = "첨부 파일 url 리스트", example = "[\"https://example.com/uploads/faq/files/abc.pdf\"]")
        List<String> fileUrls
        ) {}