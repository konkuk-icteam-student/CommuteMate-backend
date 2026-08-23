package com.better.CommuteMate.chat.application.service;

import com.better.CommuteMate.chat.application.dto.response.PostChatQueryResponse;
import com.better.CommuteMate.global.ai.rag.RagChatQueryResponse;
import com.better.CommuteMate.global.ai.rag.RagServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String FALLBACK_ANSWER = "일시적으로 챗봇을 이용할 수 없습니다. 잠시 후 다시 시도해주세요.";

    private final RagServiceClient ragServiceClient;

    public PostChatQueryResponse query(String query) {
        RagChatQueryResponse response;

        try {
            response = ragServiceClient.chatQuery(query);
        } catch (Exception e) {
            log.warn("RAG 챗봇 질의 실패, 폴백 응답 반환: {}", e.getMessage());
            return new PostChatQueryResponse(FALLBACK_ANSWER, List.of(), List.of(), false);
        }

        if (response == null) {
            log.warn("RAG 챗봇 응답이 비어 있음");
            return new PostChatQueryResponse(FALLBACK_ANSWER, List.of(), List.of(), false);
        }

        return new PostChatQueryResponse(
                response.answer(),
                toRegulationSourceDtos(response.regulationSources()),
                toFaqSourceDtos(response.faqSources()),
                response.conflictDetected()
        );
    }

    private List<PostChatQueryResponse.RegulationSourceDto> toRegulationSourceDtos(
            List<RagChatQueryResponse.RegulationSource> sources
    ) {
        if (sources == null) {
            return List.of();
        }

        return sources.stream()
                .map(s -> new PostChatQueryResponse.RegulationSourceDto(
                        s.source(), s.page(), s.chunkIndex(), s.score()
                ))
                .toList();
    }

    private List<PostChatQueryResponse.FaqSourceDto> toFaqSourceDtos(
            List<RagChatQueryResponse.FaqSource> sources
    ) {
        if (sources == null) {
            return List.of();
        }

        return sources.stream()
                .map(s -> new PostChatQueryResponse.FaqSourceDto(
                        s.faqId(), s.chunkIndex(), s.score()
                ))
                .toList();
    }
}
