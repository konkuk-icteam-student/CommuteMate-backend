package com.better.CommuteMate.organization.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.organization.application.OrganizationService;
import com.better.CommuteMate.organization.application.dto.request.PostOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
@Tag(name = "organization", description = "조직 관리 API")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(
            summary = "조직 등록",
            description = "새로운 조직을 등록합니다. 이미 존재하는 조직은 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조직 등록 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = REGISTER_ORGANIZATION_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 등록된 조직", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Response> registerOrganization(
            @RequestBody PostOrganizationRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "조직 등록 성공", organizationService.registerOrganization(request)));
    }

    @Operation(
            summary = "조직 전체 목록 조회",
            description = "전체 조직을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조직 전체 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = GET_ORGANIZATION_LIST_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Response> getOrganizationList() {
        return ResponseEntity.ok(new Response(true, "조직 전체 목록 조회 성공", organizationService.getOrganizationList()));
    }

    @Operation(
            summary = "조직 삭제",
            description = "조직을 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조직 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 organizationId"),
            @ApiResponse(responseCode = "409", description = "해당 조직에 담당자가 존재하여 삭제 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Response> deleteOrganization(
            @PathVariable Long organizationId
    ) {
        organizationService.deleteOrganization(organizationId);
        return ResponseEntity.ok(new Response(true, "조직 삭제 성공", null));
    }

    private static final String REGISTER_ORGANIZATION_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "조직 등록 성공",
              "details": {
                "timestamp": "2026-08-10T12:44:33.890Z",
                "organizationId": 1
              }
            }
            """;

    private static final String GET_ORGANIZATION_LIST_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "조직 전체 목록 조회 성공",
              "details": {
                "timestamp": "2026-08-10T12:44:33.890Z",
                "organizations": [
                  {
                    "organizationId": 1,
                    "organizationName": "정보운영팀"
                  }
                ]
              }
            }
            """;
}