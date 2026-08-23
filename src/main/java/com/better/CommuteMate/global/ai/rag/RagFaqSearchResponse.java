package com.better.CommuteMate.global.ai.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RagFaqSearchResponse(
        List<RagFaqSearchItem> results
) {
    public record RagFaqSearchItem(
            @JsonProperty("faq_id") Long faqId,
            @JsonProperty("score") Double score
    ) { }
}
