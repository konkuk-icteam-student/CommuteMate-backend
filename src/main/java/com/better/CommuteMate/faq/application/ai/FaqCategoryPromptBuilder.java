package com.better.CommuteMate.faq.application.ai;

import com.better.CommuteMate.domain.category.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FaqCategoryPromptBuilder {

    public String build(String title, String content, List<Category> categories) {

        String safeContent = content == null ? "" : content;

        String categoryList = categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        return """
                너는 FAQ 카테고리 분류 시스템이다.

                아래 카테고리 목록 중에서 가장 적절한 카테고리를 1~3개 선택해라.

                [카테고리 목록]
                %s

                [FAQ]
                제목: %s
                내용: %s

                반드시 JSON 배열 형태로 카테고리 이름만 반환해라.
                다른 설명 없이 아래 형식만 지켜라:
                ["카테고리1", "카테고리2"]
                """
                .formatted(categoryList, title, safeContent);
    }
}