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
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleSummaryResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeResponseDetail;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryListResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "날짜 yyyy-MM-dd, 시간 HH:mm, 30분 단위. addSlots/deleteSlots 중 하나만 있어도 됩니다.",
            content = @Content(schema = @Schema(implementation = WorkScheduleChangeRequest.class),
                    examples = @ExampleObject(name = "근무 일정 신청 요청", value = """
                            {
                              "addSlots": [{"date": "2026-04-06", "start": "13:00", "end": "14:30"}],
                              "deleteSlots": [{"date": "2026-04-05", "start": "09:00", "end": "10:00"}]
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 신청 결과",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "완전 성공", value = """
                                    {"isSuccess":true,"message":"신청하신 일정이 모두 등록되었습니다.","details":{"success":[{"start":"2026-04-06T13:00:00","end":"2026-04-06T14:30:00"}],"failure":[]}}
                                    """),
                            @ExampleObject(name = "일부 성공", value = """
                                    {"isSuccess":false,"message":"신청하신 일정 중 실패한 일정이 존재합니다.","details":{"success":[{"start":"2026-04-06T13:00:00","end":"2026-04-06T14:30:00"}],"failure":[{"start":"2026-04-07T09:00:00","end":"2026-04-07T10:00:00"}]}}
                                    """),
                            @ExampleObject(name = "전부 실패", value = """
                                    {"isSuccess":false,"message":"신청하신 일정이 모두 실패하였습니다.","details":{"success":[],"failure":[{"start":"2026-04-07T09:00:00","end":"2026-04-07T10:00:00"}]}}
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "해당 연월의 스케줄 설정 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"해당 연월의 스케줄 설정을 찾을 수 없습니다.","details":null}
                                    """))),
            @ApiResponse(responseCode = "422", description = "월 최대 근무 시간 초과",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"월 최대 근무 시간을 초과하였습니다.","details":{"limitHours":27,"requestedHours":33}}
                                    """)))
    })
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
    @Hidden
    @Operation(summary = "나의 근무 일정 조회", description = "특정 연/월의 나의 근무 일정을 조회합니다.")
    @GetMapping(params = {"year", "month"})
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
            @PathVariable Long scheduleId,
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
     * 근로시간 요약 조회 API (주/월 진행률 위젯용)
     */
    @Operation(
            summary = "근로시간 요약 조회",
            description = "특정 주간 범위의 주간·월간 근로시간 요약을 조회합니다. startDate와 endDate는 같은 달, 같은 주 이내여야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "근로시간 요약 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {"isSuccess":true,"message":"근로시간 요약을 조회했습니다.","details":{"startDate":"2026-04-06","endDate":"2026-04-10","week":{"label":"1주차","usedHours":0,"limitHours":13},"month":{"label":"4월 전체","usedHours":3,"limitHours":27}}}
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 조회 기간",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "시작 날짜가 종료 날짜보다 늦음", value = """
                                    {"isSuccess":false,"message":"시작 날짜는 종료 날짜보다 늦을 수 없습니다.","details":null}
                                    """),
                            @ExampleObject(name = "서로 다른 달", value = """
                                    {"isSuccess":false,"message":"조회 기간은 같은 달 이내여야 합니다.","details":null}
                                    """),
                            @ExampleObject(name = "서로 다른 주", value = """
                                    {"isSuccess":false,"message":"조회 기간은 같은 주 이내여야 합니다.","details":null}
                                    """)
                    }))
    })
    @GetMapping("/summary")
    public ResponseEntity<Response> getScheduleSummary(
            @Parameter(description = "조회 시작 날짜", example = "2026-04-06", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜", example = "2026-04-10", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        Long organizationId = userDetails.getUser().getOrganizationId();
        WorkScheduleSummaryResponse response =
                scheduleService.getScheduleSummary(userId, organizationId, startDate, endDate);
        return ResponseEntity.ok(Response.of(true, "근로시간 요약을 조회했습니다.", response));
    }

    /**
     * 근무 시간표 수정 요청 API
     */
    @Operation(
            summary = "근무 시간표 수정 요청",
            description = "초기 신청 기간 이후 근무 시간표 수정을 요청합니다. 관리자 승인 후 시간표에 반영됩니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "deleteSlots/addSlots 중 하나만 있어도 됩니다. EDIT 타입 정의는 기획 확인이 필요합니다.",
            content = @Content(schema = @Schema(implementation = WorkScheduleEditRequest.class),
                    examples = @ExampleObject(name = "근무 시간표 수정 요청", value = """
                            {
                              "deleteSlots": [{"date": "2026-04-06", "start": "13:00", "end": "14:30"}],
                              "addSlots": [{"date": "2026-04-09", "start": "13:00", "end": "14:30"}],
                              "reason": "사유 입력"
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 요청 제출 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "수정 요청 성공", value = """
                                    {"isSuccess":true,"message":"수정 요청이 제출되었습니다. 승인 후 시간표에 반영됩니다.","details":{"requestId":123,"status":"PENDING"}}
                                    """))),
            @ApiResponse(responseCode = "422", description = "월 최대 근무 시간 초과",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"월 최대 근무 시간을 초과하였습니다.","details":{"limitHours":27,"requestedHours":33}}
                                    """)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "월별 스케줄 제한 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {"isSuccess":true,"message":"월별 스케줄 제한을 조회했습니다.","details":{"scheduleYear":2026,"scheduleMonth":4,"maxConcurrentWorkers":10}}
                                    """))),
            @ApiResponse(responseCode = "404", description = "해당 월의 제한 설정 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"해당 월의 스케줄 제한 설정을 찾을 수 없습니다.","details":null}
                                    """)))
    })
    @GetMapping("/monthly-limit/{year}/{month}")
    public ResponseEntity<Response> getMonthlyLimit(
            @Parameter(description = "조회 연도", example = "2026", required = true)
            @PathVariable Integer year,
            @Parameter(description = "조회 월", example = "4", required = true)
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long organizationId = userDetails.getUser().getOrganizationId();
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
            description = "특정 연/월의 전체 근무 시간표를 30분 단위 슬롯으로 조회합니다. "
                    + "status: MY_SCHEDULE, PENDING_DELETE, PENDING_ADD, UNAVAILABLE, EMPTY. "
                    + "maxConcurrentWorkers 미설정 시 4를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "월별 근로 시간표 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {"isSuccess":true,"message":"근로 시간표를 조회했습니다.","details":{"year":2026,"month":4,"maxConcurrentWorkers":10,"totalLimitHours":27,"usedHours":10,"days":[{"date":"2026-04-06","slots":[{"start":"13:00","end":"13:30","status":"MY_SCHEDULE","currentCount":3}]}]}}
                                    """))),
            @ApiResponse(responseCode = "404", description = "해당 월의 스케줄 설정 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"isSuccess":false,"message":"해당 연월의 스케줄 설정을 찾을 수 없습니다.","details":null}
                                    """)))
    })
    @GetMapping("/{year}/{month}")
    public ResponseEntity<Response> getMonthlyScheduleView(
            @Parameter(description = "조회 연도", example = "2026", required = true)
            @PathVariable Integer year,
            @Parameter(description = "조회 월", example = "4", required = true)
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        Long organizationId = userDetails.getUser().getOrganizationId();

        WorkMonthlyScheduleResponse response =
                scheduleService.getMonthlyScheduleView(userId, organizationId, year, month);

        return ResponseEntity.ok(Response.of(true, "근로 시간표를 조회했습니다.", response));
    }

    /**
     * 날짜 범위 근무 시간표 조회 API (같은 달 이내)
     */
    @Operation(
            summary = "근무 시간표 기간별 조회",
            description = "startDate ~ endDate 범위(같은 달 이내)의 근무 시간표를 30분 단위 슬롯으로 조회합니다. "
                    + "status: MY_SCHEDULE, PENDING_DELETE, PENDING_ADD, UNAVAILABLE, EMPTY. "
                    + "maxConcurrentWorkers 미설정 시 4를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기간별 근로 시간표 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {"isSuccess":true,"message":"근로 시간표를 조회했습니다.","details":{"startDate":"2026-05-18","endDate":"2026-05-22","maxConcurrentWorkers":10,"totalLimitHours":27,"usedHours":10,"days":[{"date":"2026-05-18","slots":[{"start":"13:00","end":"13:30","status":"MY_SCHEDULE","currentCount":3},{"start":"13:30","end":"14:00","status":"EMPTY","currentCount":0}]}]}}
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 조회 기간",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "시작 날짜가 종료 날짜보다 늦음", value = """
                                    {"isSuccess":false,"message":"시작 날짜는 종료 날짜보다 늦을 수 없습니다.","details":null}
                                    """),
                            @ExampleObject(name = "서로 다른 달", value = """
                                    {"isSuccess":false,"message":"조회 기간은 같은 달 이내여야 합니다.","details":null}
                                    """)
                    }))
    })
    @GetMapping(params = {"startDate", "endDate"})
    public ResponseEntity<Response> getScheduleRangeView(
            @Parameter(description = "조회 시작 날짜", example = "2026-05-18", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜", example = "2026-05-22", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        Long organizationId = userDetails.getUser().getOrganizationId();

        WorkScheduleRangeResponse response =
                scheduleService.getScheduleRangeView(userId, organizationId, startDate, endDate);

        return ResponseEntity.ok(Response.of(true, "근로 시간표를 조회했습니다.", response));
    }
}
