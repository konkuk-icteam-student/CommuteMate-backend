package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;
    private final UserRepository userRepository;
    private final ScheduleValidator scheduleValidator;
    private final MonthlyScheduleConfigService monthlyScheduleConfigService;

    // 일정 추가
    @Transactional(noRollbackFor = {ScheduleAllFailureException.class, SchedulePartialFailureException.class})
    public ApplyScheduleResultCommand applyWorkSchedules(List<WorkScheduleCommand> slots) {
        List<WorkScheduleDTO> success = new ArrayList<>();
        List<WorkScheduleDTO> failure = new ArrayList<>();
        //TODO: 현재 시간이 근무 신청일이면 WS02(승인)이고, 근무 신청일이 아니면 WS01(신청) 으로 생성해야 함
        // 그런데 근무 신청일이 안정해져있다면? 사실 무조건 있는게 맞지만, 없을때도 따로 처리해야 할듯(서버구조의 오류)

        // 근무 신청일 내 일정 신청하는 경우 CodeType.WS02(승인), 그외는 WS01(신청)
        CodeType codeType = monthlyScheduleConfigService.
                isCurrentlyInApplyTerm()? CodeType.WS02: CodeType.WS01;

        for (WorkScheduleCommand slot : slots) {
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
        ApplyScheduleResultCommand result = new ApplyScheduleResultCommand(success, failure);

        if(success.isEmpty()){
            throw ScheduleAllFailureException.of(
                    ScheduleErrorCode.SCHEDULE_FAILURE, ScheduleResponseDetail.of(result));
        }else if(!failure.isEmpty()){
            throw SchedulePartialFailureException.of(
                    ScheduleErrorCode.SCHEDULE_PARTIAL_FAILURE, ScheduleResponseDetail.of(result));
        }
        return result;

    }
    /**
     TODO : 근무 수정 메서드만 있으면 될듯. 근무 수정에서
      근무를 빼는 요청만 있는경우는 근무 취소고, 근무를 빼는 요청과 더하는 요청 두개인 경우 취소하고 재 신청. 한번에 커버가능.
     */
}
