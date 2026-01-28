package com.better.CommuteMate.category.controller;

import com.better.CommuteMate.category.application.CategoryService;
import com.better.CommuteMate.category.application.dto.request.PostCategoryRequest;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListWrapper;
import com.better.CommuteMate.category.application.dto.response.PostCategoryResponse;
import com.better.CommuteMate.category.application.dto.request.PutCategoryUpdateRequest;
import com.better.CommuteMate.category.application.dto.response.PutCategoryUpdateResponse;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "분류 관리 API")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "category 등록",
            description = "새로운 category(분류)를 등록합니다. 이미 존재하는 것은 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리 등록 성공",
                    content = @Content(schema = @Schema(implementation = PostCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 분류명",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content)
    })    @PostMapping
    public ResponseEntity<Response> registerCategory(
            @RequestBody PostCategoryRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "카테고리 등록 성공", categoryService.registerCategory(request)));
    }

    @Operation(
            summary = "category 수정",
            description = "category name을 변경할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리 수정 성공",
                    content = @Content(schema = @Schema(implementation = PutCategoryUpdateResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 categoryId", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 category 이름", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PutMapping("/{categoryId}")
    public ResponseEntity<Response> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody PutCategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "카테고리 수정 성공", categoryService.updateCategory(categoryId, request)));
    }

    @Operation(
            summary = "category 전체 조회",
            description = "전체 category(분류)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 카테고리 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetCategoryListWrapper.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Response> getCategoryList() {
        return ResponseEntity.ok(new Response(true, "전체 카테고리 조회 성공", categoryService.getCategoryList()));
    }

    @Operation(
            summary = "category 삭제",
            description = "category를 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 categoryId"),
            @ApiResponse(responseCode = "409", description = "해당 카테고리에 FAQ 또는 담당자가 존재하여 삭제 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Response> deleteCategory(
            @PathVariable Long categoryId
    ) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(new Response(true, "카테고리 삭제 성공", null));
    }

}