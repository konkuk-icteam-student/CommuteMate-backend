package com.better.CommuteMate.team.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.team.application.TeamService;
import com.better.CommuteMate.team.application.dto.request.PostTeamRequest;
import com.better.CommuteMate.team.application.dto.response.GetTeamListWrapper;
import com.better.CommuteMate.team.application.dto.response.PostTeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
                    content = @Content(schema = @Schema(implementation = PostTeamResponse.class))),
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

    @Operation(
            summary = "소속 전체 목록 조회",
            description = "전체 소속을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소속 전체 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetTeamListWrapper.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Response> getTeamList() {
        return ResponseEntity.ok(new Response(true, "소속 전체 목록 조회 성공", teamService.getTeamList()));
    }

    @Operation(
            summary = "소속 삭제",
            description = "소속을 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소속 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 teamId"),
            @ApiResponse(responseCode = "409", description = "해당 소속에 담당자가 존재하여 삭제 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Response> deleteTeam(
            @PathVariable Long teamId
    ) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.ok(new Response(true, "소속 삭제 성공", null));
    }

}