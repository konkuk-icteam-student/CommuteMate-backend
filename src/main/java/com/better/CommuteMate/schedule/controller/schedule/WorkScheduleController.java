package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeResultCommand;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkMonthlyScheduleResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeRequest;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditRequest;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleEditResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleMonthlyLimitResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeResponseDetail;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryListResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "사용자 근무 일정", description = "사용자 근무 일정 신청 및 조회 API")
@RestController
@RequestMapping("/api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 근무 일정 변경사항만 반영합니다.
     * <p>
     * addSlots는 새로 추가할 일정, deleteSlots는 취소할 일정을 의미합니다.
     * addSlots / deleteSlots 중 하나만 있어도 요청 가능합니다.
     * </p>
     */
    @Operation(
            summary = "근무 일정 신청",
            description = "변경사항만 addSlots / deleteSlots로 전달하여 근무 일정을 추가 또는 삭제합니다."
    )
    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(
            @RequestBody WorkScheduleChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();

        WorkScheduleChangeCommand command =
                WorkScheduleChangeCommand.from(request, userId);

        WorkScheduleChangeResultCommand result =
                scheduleService.changeWorkSchedules(command);

        WorkScheduleChangeResponseDetail detail;
        boolean isSuccess;
        String message;

        if (result.isAllSuccess()) {
            isSuccess = true;
            message = "신청하신 일정이 모두 등록되었습니다.";
            detail = WorkScheduleChangeResponseDetail.allSuccess(result.success());
        } else if (result.isPartialSuccess()) {
            isSuccess = false;
            message = "신청하신 일정 중 실패한 일정이 존재합니다.";
            detail = WorkScheduleChangeResponseDetail.withFailure(
                    result.success(),
                    result.failure()
            );
        } else {
            isSuccess = false;
            message = "신청하신 일정이 모두 실패하였습니다.";
            detail = WorkScheduleChangeResponseDetail.withFailure(
                    result.success(),
                    result.failure()
            );
        }

        return ResponseEntity.ok(Response.of(
                isSuccess,
                message,
                detail
        ));
    }

    /**
     * 나의 근무 일정 조회 API (월별)
     */
    @Operation(summary = "나의 근무 일정 조회", description = "특정 연/월의 나의 근무 일정을 조회합니다.")
    @GetMapping
    public ResponseEntity<Response> getWorkSchedules(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();

        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 조회 성공",
                WorkScheduleListResponse.of(
                        scheduleService.getWorkSchedules(userId, year, month)
                )
        ));
    }

    /**
     * 근무 이력 조회 API
     */
    @Operation(summary = "근무 이력 조회", description = "특정 연/월의 근무 이력(실제 근무 포함)을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<Response> getWorkScheduleHistory(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();

        return ResponseEntity.ok(Response.of(
                true,
                "근무 이력 조회 성공",
                WorkScheduleHistoryListResponse.of(
                        scheduleService.getWorkScheduleHistory(userId, year, month)
                )
        ));
    }

    /**
     * 특정 근무 일정 상세 조회 API
     */
    @Operation(summary = "특정 근무 일정 조회", description = "ID로 특정 근무 일정을 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<Response> getWorkSchedule(
            @PathVariable String scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();

        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 상세 조회 성공",
                scheduleService.getWorkSchedule(userId, scheduleId)
        ));
    }

    /**
     * 근무 시간표 수정 요청 API
     */
    @Operation(
            summary = "근무 시간표 수정 요청",
            description = "초기 신청 기간 이후 근무 시간표 수정을 요청합니다. 관리자 승인 후 시간표에 반영됩니다."
    )
    @PostMapping("/edit")
    public ResponseEntity<Response> submitEditRequest(
            @RequestBody WorkScheduleEditRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        WorkScheduleEditResponse response = scheduleService.submitEditRequest(userId, request);
        return ResponseEntity.ok(Response.of(true, "수정 요청이 제출되었습니다. 승인 후 시간표에 반영됩니다.", response));
    }

    /**
     * 월별 스케줄 동시 근무 제한 조회 API
     */
    @Operation(summary = "월별 스케줄 동시 근무 제한 조회", description = "특정 연/월의 최대 동시 근무자 수를 조회합니다.")
    @GetMapping("/monthly-limit/{year}/{month}")
    public ResponseEntity<Response> getMonthlyLimit(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String organizationId = String.valueOf(userDetails.getUser().getOrganizationId());
        return ResponseEntity.ok(Response.of(
                true,
                "월별 스케줄 제한을 조회했습니다.",
                scheduleService.getMonthlyLimit(organizationId, year, month)
        ));
    }

    /**
     * 월별 근무 시간표 조회 API
     */
    @Operation(
            summary = "근무 시간표 월별 조회",
            description = "특정 연/월의 전체 근무 시간표를 30분 단위 슬롯으로 조회합니다."
    )
    @GetMapping("/{year}/{month}")
    public ResponseEntity<Response> getMonthlyScheduleView(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        String organizationId = String.valueOf(userDetails.getUser().getOrganizationId());

        WorkMonthlyScheduleResponse response =
                scheduleService.getMonthlyScheduleView(userId, organizationId, year, month);

        return ResponseEntity.ok(Response.of(true, "근로 시간표를 조회했습니다.", response));
    }

    /**
     * 날짜 범위 근무 시간표 조회 API (같은 달 이내)
     */
    @Operation(
            summary = "근무 시간표 기간별 조회",
            description = "startDate ~ endDate 범위(같은 달 이내)의 근무 시간표를 30분 단위 슬롯으로 조회합니다."
    )
    @GetMapping(params = {"startDate", "endDate"})
    public ResponseEntity<Response> getScheduleRangeView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        String organizationId = String.valueOf(userDetails.getUser().getOrganizationId());

        WorkScheduleRangeResponse response =
                scheduleService.getScheduleRangeView(userId, organizationId, startDate, endDate);

        return ResponseEntity.ok(Response.of(true, "근로 시간표를 조회했습니다.", response));
    }
}