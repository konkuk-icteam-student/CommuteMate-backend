package com.better.CommuteMate.manager.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.manager.application.ManagerService;
import com.better.CommuteMate.manager.application.dto.request.PostManagerRequest;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListWrapper;
import com.better.CommuteMate.manager.application.dto.response.PatchFavoriteManagerResponse;
import com.better.CommuteMate.manager.application.dto.response.PostManagerResponse;
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

    @Operation(
            summary = "담당자 등록",
            description = "새로운 담당자를 등록합니다. 이미 해당 카테고리에 등록된 담당자는 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "담당자 등록 성공",
                    content = @Content(schema = @Schema(implementation = PostManagerResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 카테고리 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 등록된 담당자", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Response> registerManager(
            @RequestBody PostManagerRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "담당자 등록 성공", managerService.registerManager(request)));
    }

    @Operation(
            summary = "담당자 목록 조회",
            description = "담당자 목록을 조회합니다. 소속, 분류, 즐겨찾기 여부로 필터링할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "담당자 목록 조회 성공", content = @Content(schema = @Schema(implementation = GetManagerListWrapper.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",  content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Response> getManagerList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "false") boolean favoriteOnly
    ) {
        return ResponseEntity.ok(new Response(true, "카테고리 담당자 목록 조회 성공", managerService.getManagerList(categoryId, teamId, favoriteOnly)));
    }


    @Operation(
            summary = "담당자 즐겨찾기 등록 및 해제",
            description = "담당자를 즐겨찾기 등록 및 해제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "담당자 즐겨찾기 등록 및 해제 성공",
                    content = @Content(schema = @Schema(implementation = PatchFavoriteManagerResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 담당자-분류 매핑 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{managerId}/category/{categoryId}")
    public ResponseEntity<Response> updateFavoriteManager(
            @PathVariable Long managerId,
            @PathVariable Long categoryId,
            @RequestParam boolean favorite
    ) {
        String message = favorite ? "즐겨찾기 등록 성공" : "즐겨찾기 해제 성공";

        return ResponseEntity.ok(new Response(true, message, managerService.updateFavorite(managerId, categoryId, favorite)));
    }


    @Operation(
            summary = "담당자 삭제",
            description = "담당자를 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "담당자 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 managerId"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{managerId}")
    public ResponseEntity<Response> deleteManager(
            @PathVariable Long managerId
    ) {
        managerService.deleteManager(managerId);
        return ResponseEntity.ok(new Response(true, "담당자 삭제 성공", null));
    }
}
