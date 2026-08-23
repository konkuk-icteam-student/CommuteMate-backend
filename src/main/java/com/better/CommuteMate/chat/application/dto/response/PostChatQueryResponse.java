package com.better.CommuteMate.chat.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "챗봇 질의 응답 DTO")
public class PostChatQueryResponse extends ResponseDetail {

    @Schema(description = "챗봇 답변")
    private final String answer;

    @Schema(description = "답변 근거가 된 규정 출처 목록")
    private final List<RegulationSourceDto> regulationSources;

    @Schema(description = "답변 근거가 된 FAQ 출처 목록")
    private final List<FaqSourceDto> faqSources;

    @Schema(description = "규정 간 충돌이 감지되었는지 여부")
    private final boolean conflictDetected;

    public PostChatQueryResponse(
            String answer,
            List<RegulationSourceDto> regulationSources,
            List<FaqSourceDto> faqSources,
            boolean conflictDetected
    ) {
        super();
        this.answer = answer;
        this.regulationSources = regulationSources;
        this.faqSources = faqSources;
        this.conflictDetected = conflictDetected;
    }

    @Schema(description = "규정 출처")
    public record RegulationSourceDto(
            @Schema(description = "규정 문서명") String source,
            @Schema(description = "페이지 번호") Integer page,
            @Schema(description = "청크 인덱스") Integer chunkIndex,
            @Schema(description = "관련도 점수") Double score
    ) { }

    @Schema(description = "FAQ 출처")
    public record FaqSourceDto(
            @Schema(description = "FAQ ID") Long faqId,
            @Schema(description = "청크 인덱스") Integer chunkIndex,
            @Schema(description = "관련도 점수") Double score
    ) { }
}
