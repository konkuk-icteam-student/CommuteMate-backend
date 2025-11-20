package com.better.CommuteMate.subcategory.controller;

import com.better.CommuteMate.subcategory.application.dto.SubCategoryService;
import com.better.CommuteMate.subcategory.application.dto.request.PostSubCategoryRegisterRequest;
import com.better.CommuteMate.subcategory.application.dto.response.PostSubCategoryRegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
@Tag(name = "Subcategory", description = "소분류 관리 API")
public class SubCategoryController {

    private final SubCategoryService subcategoryService;

    @Operation(
            summary = "subCategory 등록",
            description = "새로운 subCategory(소분류)를 등록합니다. 같은 대분류 안에 이미 존재하는 소분류는 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = PostSubCategoryRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "같은 대분류에 이미 존재하는 소분류명",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content)
    })    @PostMapping
    public ResponseEntity<PostSubCategoryRegisterResponse> registerSubCategory(
            @RequestBody PostSubCategoryRegisterRequest request
    ) {
        return ResponseEntity.ok(subcategoryService.registerSubCategory(request));
    }
}
