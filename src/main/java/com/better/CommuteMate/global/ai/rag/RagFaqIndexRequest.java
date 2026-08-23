package com.better.CommuteMate.global.ai.rag;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record RagFaqIndexRequest(
        @JsonProperty("faq_id") Long faqId,
        @JsonProperty("text") String text,
        @JsonProperty("title") String title,
        @JsonProperty("category_ids") List<Long> categoryIds,
        @JsonProperty("category_names") List<String> categoryNames,
        // 기본 ObjectMapper가 LocalDate를 [yyyy,M,d] 배열로 직렬화해 RAG 서버가 422로 거부하므로
        // ISO 문자열(yyyy-MM-dd)로 강제한다.
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("created_at") LocalDate createdAt
) { }
