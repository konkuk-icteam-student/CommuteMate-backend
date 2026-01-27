package com.better.CommuteMate.team.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.manager.application.dto.response.PostManagerResponse;
import com.better.CommuteMate.team.application.TeamService;
import com.better.CommuteMate.team.application.dto.request.PostTeamRequest;
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
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
@Tag(name = "Team", description = "소속 관리 API")
public class TeamController {

    private final TeamService teamService;

    @Operation(
            summary = "소속 등록",
            description = "새로운 소속을 등록합니다. 이미 존재하는 소속은 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소속 등록 성공",
                    content = @Content(schema = @Schema(implementation = PostManagerResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 등록된 소속", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Response> registerTeam(
            @RequestBody PostTeamRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "소속 등록 성공", teamService.registerTeam(request)));
    }
}