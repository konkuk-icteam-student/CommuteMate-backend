package com.better.CommuteMate.manager.controller;


import com.better.CommuteMate.category.application.dto.response.PostCategoryRegisterResponse;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.manager.application.ManagerService;
import com.better.CommuteMate.manager.application.dto.request.PostManagerRequest;
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
            description = "새로운 담당자를 등록합니다. 이미 존재하는 담당자는 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "담당자 등록 성공",
                    content = @Content(schema = @Schema(implementation = PostManagerResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 담당자",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content)
    })    @PostMapping
    public ResponseEntity<Response> registerManager(
            @RequestBody PostManagerRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "담당자 등록 성공", managerService.registerManager(request)));
    }
}
