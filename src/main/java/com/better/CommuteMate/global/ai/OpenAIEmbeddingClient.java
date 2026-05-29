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
public class OpenAIEmbeddingClient {

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private final WebClient webClient;

    public OpenAIEmbeddingClient(
            WebClient.Builder builder,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.embedding-url}") String url
    ) {
        this.webClient = builder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public float[] embed(String text) {

        Map<String, Object> body = Map.of(
                "model", EMBEDDING_MODEL,
                "input", text
        );

        OpenAIEmbeddingResponse response =
                webClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(OpenAIEmbeddingResponse.class)
                        .block();

        List<Float> embedding = response.data().get(0).embedding();

        float[] vector = new float[embedding.size()];

        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }

        return vector;
    }
}