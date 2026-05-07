package com.better.CommuteMate.global.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAIClient {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.url}")
    private String url;

    private final WebClient.Builder webClientBuilder;

    public String call(String prompt) {

        WebClient webClient = webClientBuilder.build();

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map response = webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return extractContent(response);
    }

    private String extractContent(Map response) {
        try {
            List choices = (List) response.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "[]"; // fallback
        }
    }
}