package com.better.CommuteMate.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAIClient {

    private final WebClient webClient;

    public OpenAIClient(
            WebClient.Builder builder,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.url}") String url
    ) {
        this.webClient = builder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String call(String prompt) {

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        OpenAICategoryResponse response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAICategoryResponse.class)
                .block();

        log.info("GPT response = {}", response);

        return extractContent(response);
    }

    private String extractContent(OpenAICategoryResponse response) {
        try {
            return response.choices().get(0).message().content();
        } catch (Exception e) {
            log.warn("GPT 카테고리 추천 응답 파싱 실패: {}", e.getMessage());
            return "[]"; // fallback
        }
    }
}