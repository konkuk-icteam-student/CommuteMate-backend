package com.better.CommuteMate.manager.controller;

import com.better.CommuteMate.category.application.dto.request.PostManagerSubCategoryRequest;
import com.better.CommuteMate.category.application.dto.response.PostManagerSubCategoryResponse;
import com.better.CommuteMate.category.application.dto.request.PutManagerSubCategoryRequest;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.manager.application.ManagerService;
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
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@Tag(name = "Manager", description = "담당자 관리 API")
public class ManagerController {

    private final ManagerService managerService;

    @Operation(summary = "매니저 권한 등록", description = "기존 유저의 역할을 매니저로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매니저 권한 등록 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 매니저 권한 보유")
    })
    @PostMapping("/{userId}")
    public ResponseEntity<Response> registerManager(@PathVariable Integer userId) {
        managerService.registerManager(userId);
        return ResponseEntity.ok(new Response(true, "매니저 등록 성공", null));
    }

    @Operation(
            summary = "manager-subCategory 매핑 등록",
            description = "manager가 담당할 subCategory를 등록합니다. 이미 매핑되어 있는 subCategory는 중복 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매핑 등록 성공",
                    content = @Content(schema = @Schema(implementation = PostManagerSubCategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 관리자 또는 subCategory"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 매핑"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<Response> registerManagerSubCategories(
            @RequestBody PostManagerSubCategoryRequest request
    ) {
        PostManagerSubCategoryResponse response = managerService.registerMappings(request);
        return ResponseEntity.ok(
                new Response(true, "manager-subCategory 매핑 등록 성공", response)
        );
    }

    @Operation(summary = "manager-category 매핑 수정", description = "매니저의 담당 subCategory 매핑을 수정합니다. 기존 매핑은 삭제되고, 새로운 subCategory 리스트로 대체됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매핑 수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매니저 또는 서브카테고리"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping // Todo 등록에서는 id로 받으면서 여기선 name으로 받음. 통일 필요
    public ResponseEntity<Response> updateManagerSubCategories(@RequestBody PutManagerSubCategoryRequest request) {
        managerService.updateManagerSubCategories(request);
        return ResponseEntity.ok(new Response(true, "manager-category 매핑이 성공적으로 수정되었습니다.", null));
    }

    @Operation(summary = "manager-category 매핑 삭제", description = "매니저의 담당 subCategory 매핑을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매핑 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매니저 또는 서브카테고리"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping ("/subcategories/{managerId}")// TODO 여러 매핑 중에 어떤 걸 선택할지를 바디로 받아야 함
    public ResponseEntity<Response> deleteManagerSubCategories(@PathVariable Integer managerId) {
        managerService.deleteManagerSubCategories(managerId);
        return ResponseEntity.ok(new Response(true, "manager-category 매핑이 정상적으로 삭제되었습니다.", null));
    }

    @Operation(
            summary = "manager 권한 해제",
            description = "manager 권한을 해제합니다. (roleCode에서 MANAGER 권한이 제거되며, 담당했던 소분류 매핑 또한 해제됩니다.)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "권한 해제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @DeleteMapping("/{managerId}")
    public ResponseEntity<Response> revokeManagerRole(@PathVariable Integer managerId) {
        managerService.revokeManagerRole(managerId);
        return ResponseEntity.ok(new Response(true, "manager 권한이 해제되었습니다.", null));
    }
}
