package com.better.CommuteMate.faq.controller;

import com.better.CommuteMate.category.application.dto.CategoryDto;
import com.better.CommuteMate.category.application.dto.request.PostAICategoryRequest;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.category.application.dto.response.PostAICategoryResponse;
import com.better.CommuteMate.faq.application.dto.request.GetFaqAISearchRequest;
import com.better.CommuteMate.faq.application.service.FaqAIService;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "FAQ AI", description = "FAQ AI 기능 관련 API")
@RestController
@RequestMapping("/api/faq/ai")
@RequiredArgsConstructor
public class FaqAIController {

    private final FaqAIService faqAIService;

    @Operation(
            summary = "FAQ 카테고리 자동 추천",
            description = "FAQ 제목과 내용을 기반으로 카테고리를 AI가 추천합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카테고리 추천 성공",
                    content = @Content(schema = @Schema(implementation = PostAICategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PostMapping("/category-recommend")
    public Response recommend(
            @RequestBody PostAICategoryRequest request
    ) {

        List<Category> categories =
                faqAIService.recommendCategory(request.title(), request.content());

        List<CategoryDto> result = categories.stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();

        return new Response(
                true,
                "카테고리 추천 성공",
                new PostAICategoryResponse(result)
        );
    }

    @Operation(
            summary = "FAQ AI 검색",
            description = """
                    키워드로 제목+내용을 검색한 후 AI가 의미적 유사성을 기준으로 결과를 재정렬합니다.
                    소속(organizationId), 분류(categoryId), 날짜(startDate~endDate) 필터를 함께 사용할 수 있습니다.
                    페이지당 10개씩 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ AI 검색 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<Response> aiSearch(
            GetFaqAISearchRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "FAQ AI 검색 성공", faqAIService.search(request)));
    }
}