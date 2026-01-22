package com.better.CommuteMate.manager.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.manager.application.ManagerService;
import com.better.CommuteMate.manager.application.dto.request.PostManagerRequest;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListWrapper;
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
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "false") boolean favoriteOnly
    ) {
        return ResponseEntity.ok(new Response(true, "카테고리 담당자 목록 조회 성공", managerService.getManagerList(categoryId, team, favoriteOnly)));
    }


}
