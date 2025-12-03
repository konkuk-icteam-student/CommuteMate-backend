package com.better.CommuteMate.schedule.application;


import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final UserRepository userRepository;
    private final ScheduleValidator scheduleValidator;
    private final MonthlyScheduleConfigService monthlyScheduleConfigService;
    private final WorkChangeRequestRepository workChangeRequestRepository;

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
            // 근무 신청일 내 일정 신청하는 경우 CodeType.WS02(승인), 그외는 WS01(신청)
            CodeType codeType = monthlyScheduleConfigService.
                isCurrentlyInApplyTerm(slot.start())? CodeType.WS02: CodeType.WS01;

            if(scheduleValidator.isScheduleInsertable(slot)){
                User user = userRepository.findByUserId(slot.userID())
                        .orElseThrow(() -> UserNotFoundException.of(
                                GlobalErrorCode.USER_NOT_FOUND, UserNotFoundResponseDetail.of(slot.userID())));
                workSchedulesRepository.save(WorkScheduleCommand.toEntity(slot, user, codeType));
                success.add(WorkScheduleDTO.from(slot));
            }else{
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

        // 일정 삭제하면서 근무 시간 계산
        for (Integer id : modifyWorkScheduleDTO.cancelScheduleIds()) {

            Optional<WorkSchedule> workSchedule = workSchedulesRepository.findById(id);



            if(workSchedule.isPresent()){
                WorkSchedule workschedule = workSchedule.get();

                // 각 일정마다 바로수정가능한 일정인지, 아니면 변경요청을 보내야하는 일정인지 검증
                CodeType codeType = monthlyScheduleConfigService
                        .isCurrentlyInApplyTerm(workschedule.getStartTime())? CodeType.WS02: CodeType.WS01;
                CodeType changeRequestStatusCode = (codeType.equals(CodeType.WS02))? CodeType.CS02: CodeType.CS01;
                CodeType changeRequestTypeCode = CodeType.CR02; // CR02: 삭제 요청

                // 삭제할 일정의 근무 시간 누적
                cancelTotalDuration = cancelTotalDuration.plus(
                        Duration.between(workschedule.getStartTime(), workschedule.getEndTime()));

                // workSchedule soft-delete 및 상태 코드 변경
                workschedule.deleteApplySchedule(userId, codeType);
                // 삭제 요청 기록 저장
                workChangeRequestRepository.save(WorkChangeRequest.builder()
                        .reason(modifyWorkScheduleDTO.reason())
                        .statusCode(changeRequestStatusCode).typeCode(changeRequestTypeCode)
                        .schedule(workSchedule.get()).user(user).updatedBy(userId)
                        .createdBy(userId).build());
            }else{
                throw ScheduleAllFailureException.of(
                        ScheduleErrorCode.SCHEDULE_FAILURE,
                        ScheduleResponseDetail.of(ApplyScheduleResultCommand
                                .fromIds(List.of(), modifyWorkScheduleDTO.cancelScheduleIds())));
            }
        }

        // 일정 추가하면서 근무 시간 계산
        for (WorkScheduleDTO slot : modifyWorkScheduleDTO.applySlots()) {
            // 추가할 일정의 근무 시간 누적
            applyTotalDuration = applyTotalDuration.plus(Duration.between(slot.start(), slot.end()));

            // 각 일정마다 바로수정가능한 일정인지, 아니면 변경요청을 보내야하는 일정인지 검증
            CodeType codeType = monthlyScheduleConfigService
                    .isCurrentlyInApplyTerm(slot.start())? CodeType.WS02: CodeType.WS01;
            CodeType changeRequestStatusCode = (codeType.equals(CodeType.WS02))? CodeType.CS02: CodeType.CS01;
                CodeType changeRequestTypeCode = CodeType.CR01; // CR01: 수정 요청

            // 근무 신청 가능 일정인지 확인
            if(scheduleValidator.isScheduleInsertable(WorkScheduleCommand.from(slot,userId))){
                WorkSchedule workSchedule = workSchedulesRepository.save(WorkScheduleCommand.toEntity(slot, user, codeType));
                // 변경 요청 기록 저장
                workChangeRequestRepository.save(WorkChangeRequest.builder()
                        .reason(modifyWorkScheduleDTO.reason())
                        .statusCode(changeRequestStatusCode).typeCode(changeRequestTypeCode)
                        .schedule(workSchedule).user(user).updatedBy(userId)
                        .createdBy(userId).build());

            }else{
                throw ScheduleAllFailureException.of(
                        ScheduleErrorCode.SCHEDULE_FAILURE,
                        ScheduleResponseDetail.of(ApplyScheduleResultCommand
                                .from(List.of(), modifyWorkScheduleDTO.applySlots())));
            }
        }

        // 마지막에 근무 시간 일치 검증 (불일치 시 트랜잭션 롤백)
        if (!cancelTotalDuration.equals(applyTotalDuration)) {
            throw ScheduleAllFailureException.of(
                    ScheduleErrorCode.WORK_DURATION_MISMATCH,
                    ScheduleResponseDetail.of(ApplyScheduleResultCommand
                            .from(List.of(), modifyWorkScheduleDTO.applySlots())));
        }


    }
}
