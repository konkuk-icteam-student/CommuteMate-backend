package com.better.CommuteMate.schedule.application;


import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.response.UserNotFoundResponseDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

import com.better.CommuteMate.schedule.controller.dtos.ScheduleUpdateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final UserRepository userRepository;
    private final ScheduleValidator scheduleValidator;
    private final MonthlyScheduleConfigService monthlyScheduleConfigService;
    private final WorkChangeRequestRepository workChangeRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /*
     일정 추가 메서드
     - 일부만 신청 성공 가능, SchedulePartialFailureException 반환
     - 전부 실패시 ScheduleAllFailureException 반환
     */
    @Transactional(noRollbackFor = {ScheduleAllFailureException.class, SchedulePartialFailureException.class})
    public ApplyScheduleResultCommand applyWorkSchedules(List<WorkScheduleCommand> slots) {
        List<WorkScheduleDTO> success = new ArrayList<>();
        List<WorkScheduleDTO> failure = new ArrayList<>();
        //TODO: 근무 신청일이 안정해져있다면? 사실 무조건 있는게 맞지만, 없을때(현재)는 WS01(신청)으로 처리하는 중. 추후 로직 수정해야 할 수도 있음.
        //TODO: 컨트롤러에서 유저 정보를 감싸면서 for문 순회하며 WorkScheduleCommand에 유저정보를 넣어주는 것보단
        // 유저 정보를 개별 파라미터로 받아서 for문 돌면서 userID만 꺼내쓰는게 더 나을 것 같음.



        for (WorkScheduleCommand slot : slots) {
            try {
                // 1. 최소 근무 시간(2시간) 검증
                scheduleValidator.validateMinWorkTime(slot);

                // 2. 월 총 근무 시간(27시간) 검증
                LocalDateTime startOfMonth = slot.start().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
                LocalDateTime endOfMonth = slot.start().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);

                List<WorkSchedule> monthlySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                        slot.userID(), startOfMonth, endOfMonth);

                long currentMonthlyMinutes = monthlySchedules.stream()
                        .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                        .sum();

                // 이번 배치 신청 건 중 같은 달의 시간 합산
                long currentBatchMonthlyMinutes = success.stream()
                        .filter(s -> s.start().getMonth() == slot.start().getMonth() && s.start().getYear() == slot.start().getYear())
                        .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes())
                        .sum();

                long newSlotMinutes = Duration.between(slot.start(), slot.end()).toMinutes();
                scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes + currentBatchMonthlyMinutes, newSlotMinutes);

                // 3. 주 최대 근무 시간(13시간) 검증
                LocalDate slotDate = slot.start().toLocalDate();
                LocalDate startOfWeekIso = slotDate.with(WeekFields.ISO.dayOfWeek(), 1); // 월요일
                LocalDateTime startOfWeekDateTime = startOfWeekIso.atStartOfDay();
                LocalDateTime endOfWeekDateTime = startOfWeekIso.plusDays(7).atStartOfDay();

                List<WorkSchedule> weeklySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                        slot.userID(), startOfWeekDateTime, endOfWeekDateTime);

                long currentWeeklyMinutes = weeklySchedules.stream()
                        .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                        .sum();

                // 이번 배치 신청 건 중 같은 주의 시간 합산
                long currentBatchWeeklyMinutes = success.stream()
                        .filter(s -> {
                            LocalDate d = s.start().toLocalDate();
                            return !d.isBefore(startOfWeekIso) && d.isBefore(startOfWeekIso.plusDays(7));
                        })
                        .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes())
                        .sum();

                scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes + currentBatchWeeklyMinutes, newSlotMinutes);

                // 4. 시간대별 최대 인원(동시 근무자 수) 검증
                if (scheduleValidator.isScheduleInsertable(slot)) {
                    // 근무 신청일 내 일정 신청하는 경우 CodeType.WS02(승인), 그외는 WS01(신청)
                    CodeType codeType = monthlyScheduleConfigService.
                            isCurrentlyInApplyTerm(slot.start()) ? CodeType.WS02 : CodeType.WS01;

                    User user = userRepository.findByUserId(slot.userID())
                            .orElseThrow(() -> UserNotFoundException.of(
                                    GlobalErrorCode.USER_NOT_FOUND, UserNotFoundResponseDetail.of(slot.userID())));
                    workSchedulesRepository.save(WorkScheduleCommand.toEntity(slot, user, codeType));
                    success.add(WorkScheduleDTO.from(slot));

                    // [추가] 스케줄 변경 사항 브로드캐스트 (WS02: 승인일 때만)
                    if (codeType.equals(CodeType.WS02)) {
                        broadcastScheduleUpdate(slot.start().toLocalDate(), "근무 신청이 승인되었습니다.");
                    }
                } else {
                    failure.add(WorkScheduleDTO.from(slot));
                }
            } catch (BasicException e) {
                // 검증 실패 시 failure 리스트에 추가
                failure.add(WorkScheduleDTO.from(slot));
            }
        }
        ApplyScheduleResultCommand result = ApplyScheduleResultCommand.from(success, failure);

        if(success.isEmpty()){
            throw ScheduleAllFailureException.of(
                    ScheduleErrorCode.SCHEDULE_FAILURE, ScheduleResponseDetail.of(result));
        }else if(!failure.isEmpty()){
            throw SchedulePartialFailureException.of(
                    ScheduleErrorCode.SCHEDULE_PARTIAL_FAILURE, ScheduleResponseDetail.of(result));
        }
        return result;

    }
    /*
     - 근무 수정 메서드
     - 근무를 빼는 요청과 더하는 요청 시간 맞아야 함.
     */
    @Transactional
    public void modifyWorkSchedules(ModifyWorkScheduleDTO modifyWorkScheduleDTO, Integer userId) {
        // 근무 신청일 내 일정 신청하는 경우 CodeType.WS02(승인), 그외는 WS01(신청)
        // csCodeType: admin이 확인하는 Request 엔티티들의 상태. WS02 -> CS02(승인), WS01 -> CS01(대기)
        // deleteCodeType: 삭제시 적용할 코드 타입. WS02 -> WS04(취소), WS01 -> CR01(수정 요청)


        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> UserNotFoundException.of(
                        GlobalErrorCode.USER_NOT_FOUND, UserNotFoundResponseDetail.of(userId)));

        // 근무 시간 누적 변수
        Duration cancelTotalDuration = Duration.ZERO;
        Duration applyTotalDuration = Duration.ZERO;

        // 삭제된 스케줄 ID 수집 (월/주 시간 계산 시 제외용)
        Set<Integer> canceledScheduleIds = new HashSet<>();

        // 일정 삭제하면서 근무 시간 계산
        for (Integer id : modifyWorkScheduleDTO.cancelScheduleIds()) {

            Optional<WorkSchedule> workSchedule = workSchedulesRepository.findById(id);



            if(workSchedule.isPresent()){
                WorkSchedule workschedule = workSchedule.get();

                // 지난 달 일정 수정 불가 검증
                LocalDate firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
                if (workschedule.getStartTime().toLocalDate().isBefore(firstDayOfCurrentMonth)) {
                    throw BasicException.of(ScheduleErrorCode.PAST_MONTH_MODIFICATION_NOT_ALLOWED);
                }

                // 각 일정마다 바로수정가능한 일정인지, 아니면 변경요청을 보내야하는 일정인지 검증
                CodeType codeType = monthlyScheduleConfigService
                        .isCurrentlyInApplyTerm(workschedule.getStartTime())? CodeType.WS02: CodeType.WS01;
                CodeType changeRequestStatusCode = (codeType.equals(CodeType.WS02))? CodeType.CS02: CodeType.CS01;
                CodeType changeRequestTypeCode = CodeType.CR02; // CR02: 삭제 요청

                // 삭제할 일정의 근무 시간 누적
                cancelTotalDuration = cancelTotalDuration.plus(
                        Duration.between(workschedule.getStartTime(), workschedule.getEndTime()));

                // 삭제된 ID 수집
                canceledScheduleIds.add(id);

                // workSchedule soft-delete 및 상태 코드 변경
                workschedule.deleteApplySchedule(userId, codeType);
                // 삭제 요청 기록 저장
                workChangeRequestRepository.save(WorkChangeRequest.builder()
                        .reason(modifyWorkScheduleDTO.reason())
                        .statusCode(changeRequestStatusCode).typeCode(changeRequestTypeCode)
                        .schedule(workSchedule.get()).user(user).updatedBy(userId)
                        .createdBy(userId).build());

                // [추가] 변경 사항 브로드캐스트 (WS02: 승인일 때만 = 즉시 취소)
                if (codeType.equals(CodeType.WS02)) {
                    broadcastScheduleUpdate(workschedule.getStartTime().toLocalDate(), "근무가 취소되었습니다.");
                }
            }else{
                throw ScheduleAllFailureException.of(
                        ScheduleErrorCode.SCHEDULE_FAILURE,
                        ScheduleResponseDetail.of(ApplyScheduleResultCommand
                                .fromIds(List.of(), modifyWorkScheduleDTO.cancelScheduleIds())));
            }
        }

        // 추가될 슬롯 수집 (배치 내 누적 계산용)
        List<WorkScheduleDTO> addedSlots = new ArrayList<>();

        // 일정 추가하면서 근무 시간 계산 및 검증
        for (WorkScheduleDTO slot : modifyWorkScheduleDTO.applySlots()) {
            // 지난 달 일정 추가 불가 검증
            LocalDate firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            if (slot.start().toLocalDate().isBefore(firstDayOfCurrentMonth)) {
                throw BasicException.of(ScheduleErrorCode.PAST_MONTH_MODIFICATION_NOT_ALLOWED);
            }

            WorkScheduleCommand command = WorkScheduleCommand.from(slot, userId);

            // 1. 최소 근무 시간(2시간) 검증
            scheduleValidator.validateMinWorkTime(command);

            // 2. 월 총 근무 시간(27시간) 검증
            LocalDateTime startOfMonth = slot.start().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
            LocalDateTime endOfMonth = slot.start().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);

            List<WorkSchedule> monthlySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                    userId, startOfMonth, endOfMonth);

            // 삭제 예정인 일정은 제외하고 월 총 근무 시간 계산
            long currentMonthlyMinutes = monthlySchedules.stream()
                    .filter(s -> !canceledScheduleIds.contains(s.getScheduleId()))
                    .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                    .sum();

            // 이번 배치에서 이미 추가된 같은 달의 시간 합산
            long currentBatchMonthlyMinutes = addedSlots.stream()
                    .filter(s -> s.start().getMonth() == slot.start().getMonth() && s.start().getYear() == slot.start().getYear())
                    .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes())
                    .sum();

            long newSlotMinutes = Duration.between(slot.start(), slot.end()).toMinutes();
            scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes + currentBatchMonthlyMinutes, newSlotMinutes);

            // 3. 주 최대 근무 시간(13시간) 검증
            LocalDate slotDate = slot.start().toLocalDate();
            LocalDate startOfWeekIso = slotDate.with(WeekFields.ISO.dayOfWeek(), 1); // 월요일
            LocalDateTime startOfWeekDateTime = startOfWeekIso.atStartOfDay();
            LocalDateTime endOfWeekDateTime = startOfWeekIso.plusDays(7).atStartOfDay();

            List<WorkSchedule> weeklySchedules = workSchedulesRepository.findValidSchedulesByUserAndDateRange(
                    userId, startOfWeekDateTime, endOfWeekDateTime);

            // 삭제 예정인 일정은 제외하고 주 총 근무 시간 계산
            long currentWeeklyMinutes = weeklySchedules.stream()
                    .filter(s -> !canceledScheduleIds.contains(s.getScheduleId()))
                    .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                    .sum();

            // 이번 배치에서 이미 추가된 같은 주의 시간 합산
            long currentBatchWeeklyMinutes = addedSlots.stream()
                    .filter(s -> {
                        LocalDate d = s.start().toLocalDate();
                        return !d.isBefore(startOfWeekIso) && d.isBefore(startOfWeekIso.plusDays(7));
                    })
                    .mapToLong(s -> Duration.between(s.start(), s.end()).toMinutes())
                    .sum();

            scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes + currentBatchWeeklyMinutes, newSlotMinutes);

            // 4. 동시 근무자 수 검증
            if (!scheduleValidator.isScheduleInsertable(command)) {
                throw ScheduleAllFailureException.of(
                        ScheduleErrorCode.SCHEDULE_FAILURE,
                        ScheduleResponseDetail.of(ApplyScheduleResultCommand
                                .from(List.of(), modifyWorkScheduleDTO.applySlots())));
            }

            // 추가할 일정의 근무 시간 누적
            applyTotalDuration = applyTotalDuration.plus(Duration.between(slot.start(), slot.end()));

            // 각 일정마다 바로수정가능한 일정인지, 아니면 변경요청을 보내야하는 일정인지 검증
            CodeType codeType = monthlyScheduleConfigService
                    .isCurrentlyInApplyTerm(slot.start())? CodeType.WS02: CodeType.WS01;
            CodeType changeRequestStatusCode = (codeType.equals(CodeType.WS02))? CodeType.CS02: CodeType.CS01;
            CodeType changeRequestTypeCode = CodeType.CR01; // CR01: 수정 요청

            // 일정 저장
            WorkSchedule workSchedule = workSchedulesRepository.save(WorkScheduleCommand.toEntity(slot, user, codeType));
            // 변경 요청 기록 저장
            workChangeRequestRepository.save(WorkChangeRequest.builder()
                    .reason(modifyWorkScheduleDTO.reason())
                    .statusCode(changeRequestStatusCode).typeCode(changeRequestTypeCode)
                    .schedule(workSchedule).user(user).updatedBy(userId)
                    .createdBy(userId).build());

            // [추가] 변경 사항 브로드캐스트 (WS02: 승인일 때만 = 즉시 승인)
            if (codeType.equals(CodeType.WS02)) {
                broadcastScheduleUpdate(slot.start().toLocalDate(), "근무 변경(추가)이 승인되었습니다.");
            }

            // 배치 내 추가된 슬롯 수집
            addedSlots.add(slot);
        }

        // 마지막에 근무 시간 일치 검증 (불일치 시 트랜잭션 롤백)
        if (!cancelTotalDuration.equals(applyTotalDuration)) {
            throw ScheduleAllFailureException.of(
                    ScheduleErrorCode.WORK_DURATION_MISMATCH,
                    ScheduleResponseDetail.of(ApplyScheduleResultCommand
                            .from(List.of(), modifyWorkScheduleDTO.applySlots())));
        }


    }

    /**
     * 특정 사용자의 연/월별 근무 일정 조회
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> getWorkSchedules(Integer userId, Integer year, Integer month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);
        return workSchedulesRepository.findValidSchedulesByUserAndDateRange(userId, start, end)
                .stream().map(WorkScheduleResponse::from).toList();
    }

    /**
     * 특정 근무 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public WorkScheduleResponse getWorkSchedule(Integer userId, Integer scheduleId) {
        WorkSchedule schedule = workSchedulesRepository.findById(scheduleId)
                .orElseThrow(() -> BasicException.of(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 본인 일정인지 확인
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw BasicException.of(ScheduleErrorCode.UNAUTHORIZED_ACCESS);
        }
        return WorkScheduleResponse.from(schedule);
    }

    /**
     * 근무 일정 삭제/취소 요청
     */
    @Transactional
    public void deleteWorkSchedule(Integer userId, Integer scheduleId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> UserNotFoundException.of(
                        GlobalErrorCode.USER_NOT_FOUND, UserNotFoundResponseDetail.of(userId)));

        WorkSchedule schedule = workSchedulesRepository.findById(scheduleId)
                .orElseThrow(() -> BasicException.of(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 본인 일정인지 확인
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw BasicException.of(ScheduleErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 지난 달 일정 삭제 불가 검증
        LocalDate firstDayOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        if (schedule.getStartTime().toLocalDate().isBefore(firstDayOfCurrentMonth)) {
            throw BasicException.of(ScheduleErrorCode.PAST_MONTH_MODIFICATION_NOT_ALLOWED);
        }

        // 신청 기간 내 삭제는 즉시 취소(WS02 -> WS04), 그 외는 삭제 요청(CS01)
        CodeType codeType = monthlyScheduleConfigService
                .isCurrentlyInApplyTerm(schedule.getStartTime()) ? CodeType.WS02 : CodeType.WS01;
        CodeType changeRequestStatusCode = (codeType.equals(CodeType.WS02)) ? CodeType.CS02 : CodeType.CS01;
        CodeType changeRequestTypeCode = CodeType.CR02; // CR02: 삭제 요청

        schedule.deleteApplySchedule(userId, codeType);
        // 삭제 요청 기록 저장
        workChangeRequestRepository.save(WorkChangeRequest.builder()
                .reason("Deleted by user")
                .statusCode(changeRequestStatusCode).typeCode(changeRequestTypeCode)
                .schedule(schedule).user(user).updatedBy(userId)
                .createdBy(userId).build());

        // [추가] 변경 사항 브로드캐스트 (WS02: 승인일 때만 = 즉시 취소)
        if (codeType.equals(CodeType.WS02)) {
            broadcastScheduleUpdate(schedule.getStartTime().toLocalDate(), "근무가 취소되었습니다.");
        }
    }

    /**
     * 모든 접속자에게 스케줄 변경 알림을 전송합니다.
     * 클라이언트는 /topic/schedule-updates 를 구독하여 이 메시지를 수신합니다.
     */
    private void broadcastScheduleUpdate(LocalDate targetDate, String message) {
        ScheduleUpdateMessage updateMessage = ScheduleUpdateMessage.builder()
                .type("SCHEDULE_UPDATED")
                .targetDate(targetDate)
                .message(message)
                .build();
        messagingTemplate.convertAndSend("/topic/schedule-updates", updateMessage);
    }
}