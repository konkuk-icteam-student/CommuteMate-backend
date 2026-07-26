package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.MonthlyScheduleSettingService;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 근무 일정 관리", description = "관리자용 근무 일정 설정 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final MonthlyScheduleSettingService monthlyScheduleSettingService;

    @PutMapping("/work-application-settings/{year}/{month}")
    @Operation(
            summary = "월별 근로신청 설정 저장",
            description = "설정을 생성 또는 수정하고 새 규칙에 맞지 않는 기존 신청을 취소합니다."
    )
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> saveScheduleSetting(
            @PathVariable int year,
            @PathVariable int month,
            @Valid @RequestBody SaveScheduleSettingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String organizationId = String.valueOf(userDetails.getUser().getOrganizationId());
        String adminId = String.valueOf(userDetails.getUserId());
        SaveScheduleSettingResponse result = monthlyScheduleSettingService.save(
                organizationId, year, month, request, adminId
        );
        return ResponseEntity.ok(Response.of(
                true,
                "근로신청 설정을 저장했습니다.",
                result
        ));
    }
}
