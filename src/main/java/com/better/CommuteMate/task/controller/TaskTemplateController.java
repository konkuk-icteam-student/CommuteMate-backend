package com.better.CommuteMate.task.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.task.application.TaskTemplateService;
import com.better.CommuteMate.task.controller.dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/task-templates")
@RequiredArgsConstructor
@Tag(name = "Task Template", description = "업무 템플릿 관리 API")
public class TaskTemplateController {

        private final TaskTemplateService templateService;

        @Operation(summary = "템플릿 목록 조회", description = "등록된 업무 템플릿 목록을 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "조회 성공")
        })
        @GetMapping
        public ResponseEntity<Response> getTemplates(
                        @Parameter(description = "활성화된 템플릿만 조회", example = "true") @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
                List<TemplateListResponse> templates = templateService.getTemplates(activeOnly);
                return ResponseEntity.ok(new Response(true, "템플릿 목록을 조회했습니다.",
                                new TemplateListWrapper(templates)));
        }

        @Operation(summary = "템플릿 상세 조회", description = "특정 템플릿의 상세 정보와 항목들을 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
        })
        @GetMapping("/{templateId}")
        public ResponseEntity<Response> getTemplate(
                        @PathVariable Long templateId) {
                TemplateDetailResponse response = templateService.getTemplate(templateId);
                return ResponseEntity.ok(new Response(true, "템플릿을 조회했습니다.", response));
        }

        @Operation(summary = "템플릿 생성", description = "새로운 업무 템플릿을 생성합니다. (관리자 전용)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "생성 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "409", description = "템플릿 이름 중복")
        })
        @PreAuthorize("hasRole('RL02')")
        @PostMapping
        public ResponseEntity<Response> createTemplate(
                        @Valid @RequestBody CreateTemplateRequest request,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer currentUserId = userDetails.getUser().getUserId();
                TemplateDetailResponse response = templateService.createTemplate(request, currentUserId);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new Response(true, "템플릿이 생성되었습니다.", response));
        }

        @Operation(summary = "템플릿 수정", description = "기존 템플릿의 정보를 수정합니다. items가 제공되면 기존 항목을 교체합니다. (관리자 전용)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "수정 성공"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
                        @ApiResponse(responseCode = "409", description = "템플릿 이름 중복")
        })
        @PreAuthorize("hasRole('RL02')")
        @PutMapping("/{templateId}")
        public ResponseEntity<Response> updateTemplate(
                        @PathVariable Long templateId,
                        @Valid @RequestBody UpdateTemplateRequest request,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer currentUserId = userDetails.getUser().getUserId();
                TemplateDetailResponse response = templateService.updateTemplate(templateId, request, currentUserId);
                return ResponseEntity.ok(new Response(true, "템플릿이 수정되었습니다.", response));
        }

        @Operation(summary = "템플릿 삭제", description = "템플릿을 삭제합니다. (관리자 전용)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "삭제 성공"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
        })
        @PreAuthorize("hasRole('RL02')")
        @DeleteMapping("/{templateId}")
        public ResponseEntity<Response> deleteTemplate(
                        @PathVariable Long templateId) {
                templateService.deleteTemplate(templateId);
                return ResponseEntity.ok(new Response(true, "템플릿이 삭제되었습니다.", null));
        }

        @Operation(summary = "템플릿 활성화/비활성화", description = "템플릿의 활성화 상태를 변경합니다. (관리자 전용)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
        })
        @PreAuthorize("hasRole('RL02')")
        @PatchMapping("/{templateId}/active")
        public ResponseEntity<Response> setTemplateActive(
                        @PathVariable Long templateId,
                        @RequestParam boolean isActive,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer currentUserId = userDetails.getUser().getUserId();
                TemplateListResponse response = templateService.setTemplateActive(templateId, isActive, currentUserId);
                String message = isActive ? "템플릿이 활성화되었습니다." : "템플릿이 비활성화되었습니다.";
                return ResponseEntity.ok(new Response(true, message, response));
        }

        @Operation(summary = "템플릿 적용", description = "템플릿을 특정 날짜에 적용하여 업무를 일괄 생성합니다. (관리자 전용)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "적용 성공"),
                        @ApiResponse(responseCode = "400", description = "템플릿에 항목이 없음"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "404", description = "템플릿 또는 담당자를 찾을 수 없음")
        })
        @PreAuthorize("hasRole('RL02')")
        @PostMapping("/{templateId}/apply")
        public ResponseEntity<Response> applyTemplate(
                        @PathVariable Long templateId,
                        @Valid @RequestBody ApplyTemplateRequest request,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer currentUserId = userDetails.getUser().getUserId();
                ApplyTemplateResponse response = templateService.applyTemplate(templateId, request, currentUserId);
                String message = String.format("템플릿이 적용되어 %d개의 업무가 생성되었습니다.", response.getCreatedCount());
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new Response(true, message, response));
        }

        // 내부 클래스: 목록 응답 래퍼
        @lombok.Getter
        private static class TemplateListWrapper extends com.better.CommuteMate.global.controller.dtos.ResponseDetail {
                private final List<TemplateListResponse> templates;

                public TemplateListWrapper(List<TemplateListResponse> templates) {
                        super();
                        this.templates = templates;
                }
        }
}
