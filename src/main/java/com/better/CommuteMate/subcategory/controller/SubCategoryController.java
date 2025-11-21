package com.better.CommuteMate.subcategory.controller;

import com.better.CommuteMate.subcategory.application.dto.SubCategoryService;
import com.better.CommuteMate.subcategory.application.dto.request.PostSubCategoryRegisterRequest;
import com.better.CommuteMate.subcategory.application.dto.request.PostSubCategoryUpdateNameRequest;
import com.better.CommuteMate.subcategory.application.dto.response.DeleteSubCategoryResponse;
import com.better.CommuteMate.subcategory.application.dto.response.PatchFavoriteSubCategoryResponse;
import com.better.CommuteMate.subcategory.application.dto.response.PostSubCategoryRegisterResponse;
import com.better.CommuteMate.subcategory.application.dto.response.PostSubCategoryUpdateNameResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
@Tag(name = "Subcategory", description = "소분류 관리 API")
public class SubCategoryController {

    private final SubCategoryService subcategoryService;

    @Operation(
            summary = "subCategory 등록",
            description = "새로운 subCategory(소분류)를 등록합니다. 같은 대분류 내 이미 존재하는 소분류는 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소분류 등록 성공",
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

    @Operation(
            summary = "subCategory 이름 수정",
            description = "subCategory의 이름을 수정합니다. 같은 대분류 내 이미 존재하는 이름으로는 변경할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소분류 이름 수정 성공",
                    content = @Content(schema = @Schema(implementation = PostSubCategoryUpdateNameResponse.class))),
            @ApiResponse(responseCode = "404", description = "소분류 ID 없음"),
            @ApiResponse(responseCode = "409", description = "같은 대분류에 이미 존재하는 이름"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{subCategoryId}/name")
    public ResponseEntity<PostSubCategoryUpdateNameResponse> updateSubCategoryName(
            @PathVariable Long subCategoryId,
            @RequestBody PostSubCategoryUpdateNameRequest request
    ) {
        return ResponseEntity.ok(subcategoryService.updateSubCategoryName(subCategoryId, request));
    }

    @Operation(
            summary = "SubCategory 삭제",
            description = "해당 subCategory를 삭제합니다. 만약 해당 subCategory에 속한 FAQ가 존재하면 삭제 불가 메시지가 반환됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소분류 삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteSubCategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "소분류 ID 없음"),
            @ApiResponse(responseCode = "409", description = "FAQ 존재로 삭제 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{subCategoryId}")
    public ResponseEntity<DeleteSubCategoryResponse> deleteSubCategory(
            @PathVariable Long subCategoryId
    ) {
        return ResponseEntity.ok(subcategoryService.deleteSubCategory(subCategoryId));
    }

    @Operation(
            summary = "SubCategory 즐겨찾기 등록 및 해제",
            description = "SubCategory를 즐겨찾기 등록 및 해제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 등록 성공",
                    content = @Content(schema = @Schema(implementation = PatchFavoriteSubCategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "소분류 ID 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{subCategoryId}")
    public ResponseEntity<PatchFavoriteSubCategoryResponse> updateFavoriteSubCategory(
            @PathVariable Long subCategoryId,
            @RequestParam boolean favorite
    ) {
        return ResponseEntity.ok(subcategoryService.updateFavorite(subCategoryId, favorite));
    }


}
