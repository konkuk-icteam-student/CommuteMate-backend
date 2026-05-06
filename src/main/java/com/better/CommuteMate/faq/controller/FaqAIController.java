package com.better.CommuteMate.faq.controller;

import com.better.CommuteMate.category.application.dto.CategoryDto;
import com.better.CommuteMate.category.application.dto.request.PostAICategoryRequest;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.category.application.dto.response.PostAICategoryResponse;
import com.better.CommuteMate.faq.application.service.FaqAICategoryService;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FAQ AI", description = "FAQ AI 기능 관련 API")
@RestController
@RequestMapping("/api/faq/ai")
@RequiredArgsConstructor
public class FaqAIController {

    private final FaqAICategoryService recommendService;

    @Operation(
            summary = "FAQ 카테고리 자동 추천",
            description = "FAQ 제목과 내용을 기반으로 카테고리를 AI가 추천합니다."
    )
    @PostMapping("/category-recommend")
    public Response recommend(
            @RequestBody PostAICategoryRequest request
    ) {

        List<Category> categories =
                recommendService.recommend(request.title(), request.content());

        List<CategoryDto> result = categories.stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();

        return new Response(
                true,
                "카테고리 추천 성공",
                new PostAICategoryResponse(result)
        );
    }
}