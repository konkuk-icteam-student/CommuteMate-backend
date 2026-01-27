package com.better.CommuteMate.schedule.controller.admin;

import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.AdminScheduleService;
import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleConfigCommand;
import com.better.CommuteMate.schedule.application.dtos.SetApplyTermCommand;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.MonthlyLimitResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.MonthlyLimitsResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessChangeRequestRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.SetMonthlyLimitRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ApplyTermResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.SetApplyTermRequest;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkTimeSummaryResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryListResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminUserWorkTimeResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

import com.better.CommuteMate.schedule.controller.admin.dtos.ApplyRequestResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.ApplyRequestListResponse;


@Tag(name = "관리자 근무 일정 관리", description = "관리자 전용 근무 일정 설정 및 변경 요청 처리 API")
@RestController
@RequestMapping("api/v1/admin/schedule")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final MonthlyScheduleConfigService monthlyScheduleConfigService;
    private final AdminScheduleService adminScheduleService;

    /**
     * 특정 연도/월의 최대 동시 근무 인원수를 설정합니다.
     * <p>
     * 이미 설정값이 존재하는 경우 업데이트하며, 없을 경우 새로 생성합니다.
     * </p>
     *
     * @param request 설정할 연도, 월, 최대 인원수 정보
     * @param userDetails 설정을 요청한 관리자 정보
     * @return 설정된 결과 {@link MonthlyLimitResponse}
     */
    @Operation(summary = "월별 스케줄 제한 설정", description = "특정 연도/월의 최대 동시 근무 인원수를 설정합니다. 이미 존재하는 경우 업데이트합니다.")
    @PostMapping("/monthly-limit")
    public ResponseEntity<Response> setMonthlyLimit(
            @RequestBody SetMonthlyLimitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();

        MonthlyScheduleConfig result = monthlyScheduleConfigService.setMonthlyLimit(
                MonthlyScheduleConfigCommand.from(
                        request.scheduleYear(),
                        request.scheduleMonth(),
                        request.maxConcurrent(),
                        userId
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(Response.of(
                true,
                "월별 스케줄 제한이 설정되었습니다.",
                MonthlyLimitResponse.from(result)
        ));
    }

    /**
     * 특정 연도/월의 스케줄 제한 설정을 조회합니다.
     *
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 해당 월의 제한 설정 정보 {@link MonthlyLimitResponse}
     */
    @Operation(summary = "특정 월 스케줄 제한 조회", description = "특정 연도/월의 스케줄 제한 설정을 조회합니다.")
    @GetMapping("/monthly-limit/{year}/{month}")
    public ResponseEntity<Response> getMonthlyLimit(
            @Parameter(description = "연도 (예: 2025)") @PathVariable Integer year,
            @Parameter(description = "월 (1-12)") @PathVariable Integer month) {

        return monthlyScheduleConfigService.getMonthlyLimit(year, month)
                .map(limit -> ResponseEntity.ok(Response.of(
                        true,
                        "월별 스케줄 제한을 조회했습니다.",
                        MonthlyLimitResponse.from(limit)
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.of(
                        false,
                        "해당 월의 스케줄 제한 설정을 찾을 수 없습니다.",
                        null
                )));
    }

    /**
     * 저장된 모든 월별 스케줄 제한 설정을 조회합니다.
     *
     * @return 모든 월별 제한 설정 리스트 {@link MonthlyLimitsResponse}
     */
    @Operation(summary = "모든 월별 스케줄 제한 조회", description = "저장된 모든 월별 스케줄 제한 설정을 조회합니다.")
    @GetMapping("/monthly-limits")
    public ResponseEntity<Response> getAllMonthlyLimits() {
        List<MonthlyScheduleConfig> limits = monthlyScheduleConfigService.getAllMonthlyLimits();

        return ResponseEntity.ok(Response.of(
                true,
                "모든 월별 스케줄 제한을 조회했습니다.",
                MonthlyLimitsResponse.from(limits)
        ));
    }

    /**
     * 특정 사용자의 월별 총 근무 시간을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 사용자의 총 근무 분(minutes) 정보 {@link UserWorkTimeResponse}
     */
    @Operation(summary = "사용자별 근무 시간 조회", description = "특정 사용자의 월별 근무 시간을 조회합니다.")
    @GetMapping("/work-time")
    public ResponseEntity<Response> getUserWorkTime(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        UserWorkTimeResponse response = adminScheduleService.getUserWorkTime(userId, year, month);
        return ResponseEntity.ok(Response.of(true, "사용자 근무 시간 조회 성공", response));
    }

    /**
     * 특정 월의 모든 사용자 근무 시간 통계를 조회합니다.
     *
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 사용자별 근무 시간 통계 리스트 {@link AdminWorkTimeSummaryResponse}
     */
    @Operation(summary = "전체 근무 시간 통계", description = "특정 월의 전체 사용자 근무 시간을 조회합니다.")
    @GetMapping("/work-time/summary")
    public ResponseEntity<Response> getWorkTimeSummary(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        List<AdminUserWorkTimeResponse> response = adminScheduleService.getWorkTimeSummary(year, month);
        return ResponseEntity.ok(Response.of(true, "전체 근무 시간 통계 조회 성공", AdminWorkTimeSummaryResponse.of(response)));
    }

    /**
     * 특정 사용자의 월별 근무 이력(스케줄 및 실제 출퇴근 시간)을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 근무 이력 리스트 {@link WorkScheduleHistoryListResponse}
     */
    @Operation(summary = "사용자별 근무 이력 조회", description = "특정 사용자의 월별 근무 이력을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<Response> getUserWorkHistory(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(Response.of(
                true,
                "사용자 근무 이력 조회 성공",
                WorkScheduleHistoryListResponse.of(adminScheduleService.getUserWorkHistory(userId, year, month))
        ));
    }

    /**
     * 특정 월의 전체 사용자 근무 이력을 조회합니다.
     *
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 전체 근무 이력 리스트 {@link WorkScheduleHistoryListResponse}
     */
    @Operation(summary = "전체 근무 이력 조회", description = "특정 월의 전체 근무 이력을 조회합니다.")
    @GetMapping("/history/all")
    public ResponseEntity<Response> getAllWorkHistory(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(Response.of(
                true,
                "전체 근무 이력 조회 성공",
                WorkScheduleHistoryListResponse.of(adminScheduleService.getAllWorkHistory(year, month))
        ));
    }

    /**
     * 특정 연도/월의 근무 신청 가능 기간을 설정합니다.
     * <p>
     * 해당 월의 설정이 미존재할 경우 자동 생성하며, 시작 시간이 종료 시간보다 늦을 수 없습니다.
     * </p>
     *
     * @param request 설정할 연도, 월, 시작/종료 일시
     * @param userDetails 설정을 요청한 관리자 정보
     * @return 설정된 신청 기간 정보 {@link ApplyTermResponse}
     */
    @Operation(summary = "신청 기간 설정", description = "특정 연도/월의 근무신청 가능 기간을 설정합니다. 미존재 시 자동 생성됩니다.")
    @PostMapping("/set-apply-term")
    public ResponseEntity<Response> setApplyTerm(
            @RequestBody SetApplyTermRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();

        MonthlyScheduleConfig result = monthlyScheduleConfigService.setApplyTerm(
                SetApplyTermCommand.from(
                        request.scheduleYear(),
                        request.scheduleMonth(),
                        request.applyStartTime(),
                        request.applyEndTime(),
                        userId
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(Response.of(
                true,
                "신청 기간이 설정되었습니다.",
                ApplyTermResponse.from(result)
        ));
    }

    /**
     * 근무 일정 변경 요청을 일괄 승인 또는 거부합니다.
     * <p>
     * requestIds는 반드시 짝수 개수여야 한다는 제약이 있습니다 (검증 로직 포함).
     * </p>
     *
     * @param request 처리할 요청 ID 목록 및 상태 코드(승인/거부)
     * @param userDetails 처리를 수행하는 관리자 정보
     * @return 처리 결과 메시지
     */
    @Operation(summary = "변경 요청 처리", description = "근무 일정 변경 요청을 승인 또는 거부합니다. 여러 요청을 일괄 처리할 수 있습니다. (requestIds 개수는 반드시 짝수)")
    @PostMapping("/process-change-request")
    public ResponseEntity<Response> processChangeRequest(
            @RequestBody ProcessChangeRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long adminId = userDetails.getUser().getUserId();

        // requestIds 개수가 짝수인지 검증
        if (request.requestIds().size() % 2 != 0) {
            throw ScheduleAllFailureException.of(
                    ScheduleErrorCode.INVALID_REQUEST_IDS_COUNT,
                    null
            );
        }

        adminScheduleService.processChangeRequest(
                request.requestIds(),
                request.statusCode(),
                adminId
        );

        String message = request.statusCode()
                .equals(CodeType.CS02)
                ? "변경 요청이 승인되었습니다."
                : "변경 요청이 거부되었습니다.";

        return ResponseEntity.status(HttpStatus.OK).body(Response.of(
                true,
                message,
                null
        ));
    }

    /**
     * 승인 대기 중인(WS01) 모든 근무 신청 목록을 조회합니다.
     *
     * @return 근무 신청 목록 리스트 {@link ApplyRequestListResponse}
     */
    @Operation(summary = "근무 신청 요청 목록 조회", description = "승인 대기 중인(WS01) 근무 신청 목록을 조회합니다.")
    @GetMapping("/apply-requests")
    public ResponseEntity<Response> getApplyRequests() {
        return ResponseEntity.ok(Response.of(
                true,
                "근무 신청 요청 목록을 조회했습니다.",
                ApplyRequestListResponse.of(adminScheduleService.getApplyRequests())
        ));
    }
}