package com.better.CommuteMate.faq.application.service;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.faq.application.ai.FaqCategoryPromptBuilder;
import com.better.CommuteMate.faq.application.ai.FaqCategoryResponseParser;
import com.better.CommuteMate.global.ai.OpenAIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqAICategoryService {

    private final CategoryRepository categoryRepository;
    private final OpenAIClient openAIClient;
    private final FaqCategoryPromptBuilder promptBuilder;
    private final FaqCategoryResponseParser parser;

    public List<Category> recommend(String title, String content) {

        List<Category> categories = categoryRepository.findAll();

        String prompt = promptBuilder.build(title, content, categories);

        String response = openAIClient.call(prompt);

        List<String> names = parser.parse(response);

        return categoryRepository.findByNameIn(names);
    }
}
