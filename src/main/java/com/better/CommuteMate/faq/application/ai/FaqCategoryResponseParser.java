package com.better.CommuteMate.faq.application.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FaqCategoryResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> parse(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("AI 카테고리 추천 응답 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}