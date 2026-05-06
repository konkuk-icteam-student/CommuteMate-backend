package com.better.CommuteMate.faq.application.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FaqCategoryResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> parse(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}