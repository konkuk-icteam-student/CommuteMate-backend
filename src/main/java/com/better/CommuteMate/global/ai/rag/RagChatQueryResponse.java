package com.better.CommuteMate.global.ai.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RagChatQueryResponse(
        String answer,
        @JsonProperty("regulation_sources") List<RegulationSource> regulationSources,
        @JsonProperty("faq_sources") List<FaqSource> faqSources,
        @JsonProperty("conflict_detected") boolean conflictDetected
) {
    public record RegulationSource(
            String source,
            Integer page,
            @JsonProperty("chunk_index") Integer chunkIndex,
            Double score
    ) { }

    public record FaqSource(
            @JsonProperty("faq_id") Long faqId,
            @JsonProperty("chunk_index") Integer chunkIndex,
            Double score
    ) { }
}
