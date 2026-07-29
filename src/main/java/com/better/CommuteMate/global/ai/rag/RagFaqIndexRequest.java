package com.better.CommuteMate.global.ai.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record RagFaqIndexRequest(
        @JsonProperty("faq_id") Long faqId,
        @JsonProperty("text") String text,
        @JsonProperty("title") String title,
        @JsonProperty("category_ids") List<Long> categoryIds,
        @JsonProperty("category_names") List<String> categoryNames,
        @JsonProperty("created_at") LocalDate createdAt
) { }
