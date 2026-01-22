package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkScheduleResponseDetail;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryListResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleListResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 근무 일정", description = "사용자 근무 일정 신청 및 수정 API")
@RestController
@RequestMapping("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 여러 근무 시간대를 일괄 신청합니다.
     * <p>
     * 신청 기간 내라면 자동 승인되며, 기간 외라면 승인 대기 상태로 등록됩니다.
     * </p>
     *
     * @param request 신청할 시간대 목록 {@link ApplyWorkSchedule}
     * @param userDetails 인증된 사용자 정보
     * @return 신청 결과 상세 (성공/실패 건수 포함) {@link ApplyWorkScheduleResponseDetail}
     */
    @Operation(summary = "근무 일정 신청", description = "여러 근무 시간대를 일괄 신청합니다.")
    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(
            @RequestBody ApplyWorkSchedule request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        ApplyScheduleResultCommand applyResult = scheduleService.applyWorkSchedules(
                request.slots().stream().map(slot -> new WorkScheduleCommand(
                        userId, slot.start(), slot.end()
                )).toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 등록되었습니다.",
                ApplyWorkScheduleResponseDetail.from(applyResult)
        ));
    }
    /**
     * 기존 근무 일정을 수정합니다.
     * <p>
     * 기존 일정을 취소 요청하고, 새로운 일정을 추가 신청하는 방식으로 동작합니다.
     * </p>
     *
     * @param request 수정할 내용 (취소할 ID 목록, 추가할 시간대 목록) {@link ModifyWorkScheduleDTO}
     * @param userDetails 인증된 사용자 정보
     * @return 수정 요청 성공 메시지
     */
    @Operation(summary = "근무 일정 수정", description = "기존 근무 일정을 취소하면서 새로운 일정을 추가 신청합니다. 취소는 schedule ID로, 추가는 시간대로 요청합니다.")
    @PatchMapping("/modify")
    public ResponseEntity<Response> modifyWorkSchedule(
            @RequestBody ModifyWorkScheduleDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
         scheduleService.modifyWorkSchedules(request, userId);
         return ResponseEntity.status(HttpStatus.CREATED).body(Response.of(
                true,
                "신청하신 일정이 모두 수정(요청)되었습니다.",
                null
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 조회 성공",
                WorkScheduleListResponse.of(scheduleService.getWorkSchedules(userId, year, month))
        ));
    }

    @Operation(summary = "근무 이력 조회", description = "특정 연/월의 근무 이력(실제 근무 포함)을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<Response> getWorkScheduleHistory(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Response.of(
                true,
                "근무 이력 조회 성공",
                WorkScheduleHistoryListResponse.of(
                        scheduleService.getWorkScheduleHistory(userDetails.getUser().getUserId(), year, month))
        ));
    }

    /**
     * 특정 근무 일정 상세 조회 API
     */
    @Operation(summary = "특정 근무 일정 조회", description = "ID로 특정 근무 일정을 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<Response> getWorkSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 상세 조회 성공",
                scheduleService.getWorkSchedule(userId, scheduleId)
        ));
    }

    /**
     * 근무 일정 취소/삭제 API
     */
    @Operation(summary = "근무 일정 취소/삭제", description = "특정 근무 일정을 취소하거나 삭제 요청을 보냅니다.")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Response> deleteWorkSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        scheduleService.deleteWorkSchedule(userId, scheduleId);
        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 취소(요청) 성공",
                null
        ));
    }

}
