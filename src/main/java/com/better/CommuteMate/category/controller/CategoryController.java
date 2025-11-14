package com.better.CommuteMate.category.controller;

import com.better.CommuteMate.category.application.CategoryService;
import com.better.CommuteMate.category.application.dto.PostCategoryRegisterRequest;
import com.better.CommuteMate.category.application.dto.PostCategoryRegisterResponse;
import com.better.CommuteMate.category.application.dto.PutCategoryUpdateRequest;
import com.better.CommuteMate.category.application.dto.PutCategoryUpdateResponse;
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
@Tag(name = "Category", description = "대분류 관리 API")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "category 등록",
            description = "새로운 category(대분류)를 등록합니다. 이미 존재하는 것은 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = PostCategoryRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 대분류명",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content)
    })    @PostMapping
    public ResponseEntity<PostCategoryRegisterResponse> registerCategory(
            @RequestBody PostCategoryRegisterRequest request
    ) {
        return ResponseEntity.ok(categoryService.registerCategory(request));
    }

    @Operation(
            summary = "category 수정",
            description = "category name을 변경할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = PutCategoryUpdateResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 categoryId", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 category 이름", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PutMapping("/{categoryId}")
    public ResponseEntity<PutCategoryUpdateResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody PutCategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }

    @Operation(
            summary = "category 삭제",
            description = """
                    category를 삭제할 수 있습니다.
                    *해당 category에 속한 subCategory가 있을 경우, 삭제 불가 메시지가 응답으로 전달됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 불가 (subCategory 존재)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 categoryId"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId
    ) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok().build();
    }
}