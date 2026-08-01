package com.better.CommuteMate.global.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * RAG 서비스(FastAPI) 호출 클라이언트.
 *
 * FAQ 청킹/임베딩/검색과 카테고리 추천 LLM 호출을 모두
 * RAG 서비스 하나로 위임한다. (Ollama는 RAG 서비스 내부 구현)
 */
@Slf4j
@Component
public class RagServiceClient {

    private final WebClient webClient;
    private final Duration requestTimeout;
    private final Duration llmTimeout;

    public RagServiceClient(
            WebClient.Builder builder,
            @Value("${rag-service.url}") String baseUrl,
            @Value("${rag-service.timeout-ms:10000}") long timeoutMs,
            @Value("${rag-service.llm-timeout-ms:120000}") long llmTimeoutMs
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.llmTimeout = Duration.ofMillis(llmTimeoutMs);
    }

    public void indexFaq(RagFaqIndexRequest request) {
        webClient.post()
                .uri("/api/v1/faqs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .timeout(llmTimeout)
                .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                .block();
    }

    public void deleteFaq(Long faqId) {
        webClient.delete()
                .uri("/api/v1/faqs/{faqId}", faqId)
                .retrieve()
                .toBodilessEntity()
                .timeout(requestTimeout)
                .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)))
                .block();
    }

    public RagFaqSearchResponse searchFaqs(
            String keyword,
            List<Long> categoryIds,
            LocalDate startDate,
            LocalDate endDate,
            int topK
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/faqs/search")
                            .queryParam("q", keyword)
                            .queryParam("top_k", topK);

                    if (categoryIds != null && !categoryIds.isEmpty()) {
                        uriBuilder.queryParam("category_ids", categoryIds.toArray());
                    }
                    if (startDate != null) {
                        uriBuilder.queryParam("date_from", startDate);
                    }
                    if (endDate != null) {
                        uriBuilder.queryParam("date_to", endDate);
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(RagFaqSearchResponse.class)
                .timeout(requestTimeout)
                .block();
    }

    public RagChatQueryResponse chatQuery(String query) {
        return webClient.post()
                .uri("/api/v1/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatQueryRequest(query))
                .retrieve()
                .bodyToMono(RagChatQueryResponse.class)
                .timeout(llmTimeout)
                .block();
    }

    public String recommendCategory(String prompt) {
        RagCategoryRecommendResponse response = webClient.post()
                .uri("/api/v1/category/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRecommendRequest(prompt))
                .retrieve()
                .bodyToMono(RagCategoryRecommendResponse.class)
                .timeout(llmTimeout)
                .block();

        if (response == null || response.response() == null) {
            log.warn("RAG 카테고리 추천 응답이 비어 있음");
            return "[]";
        }

        return response.response();
    }

    private record CategoryRecommendRequest(String prompt) { }

    private record ChatQueryRequest(String query) { }
}
