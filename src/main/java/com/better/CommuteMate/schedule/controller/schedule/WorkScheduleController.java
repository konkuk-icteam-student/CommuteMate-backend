package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkScheduleResponseDetail;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleListResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 근무 일정", description = "사용자 근무 일정 신청 및 수정 API")
@RestController
@RequestMapping("api/v1/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "근무 일정 신청", description = "여러 근무 시간대를 일괄 신청합니다.")
    @PostMapping("/apply")
    public ResponseEntity<Response> applyWorkSchedule(
            @RequestBody ApplyWorkSchedule request,
            @RequestHeader Integer userId) {
        // @RequestHeader int userID는 추후 인증로직이 추가되면 제거할 예정
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
    @Operation(summary = "근무 일정 수정", description = "기존 근무 일정을 취소하면서 새로운 일정을 추가 신청합니다. 취소는 schedule ID로, 추가는 시간대로 요청합니다.")
    @PatchMapping("/modify")
    public ResponseEntity<Response> modifyWorkSchedule(
            @RequestBody ModifyWorkScheduleDTO request,
            @RequestHeader Integer userId) {
        // @RequestHeader int userID는 추후 인증로직이 추가되면 제거할 예정
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
            @RequestHeader Integer userId) {
        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 조회 성공",
                WorkScheduleListResponse.of(scheduleService.getWorkSchedules(userId, year, month))
        ));
    }

    /**
     * 특정 근무 일정 상세 조회 API
     */
    @Operation(summary = "특정 근무 일정 조회", description = "ID로 특정 근무 일정을 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<Response> getWorkSchedule(
            @PathVariable Integer scheduleId,
            @RequestHeader Integer userId) {
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
            @PathVariable Integer scheduleId,
            @RequestHeader Integer userId) {
        scheduleService.deleteWorkSchedule(userId, scheduleId);
        return ResponseEntity.ok(Response.of(
                true,
                "근무 일정 취소(요청) 성공",
                null
        ));
    }

}
