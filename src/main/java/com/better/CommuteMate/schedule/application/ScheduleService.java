package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.schedule.application.exceptions.response.ScheduleResponseDetail;
import com.better.CommuteMate.schedule.controller.dtos.WorkScheduleDTO;
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

    // 일정 추가
    @Transactional(noRollbackFor = {ScheduleAllFailureException.class, SchedulePartialFailureException.class})
    public ApplyScheduleResultCommand applyWorkSchedules(List<WorkScheduleCommand> slots) {
        List<WorkScheduleDTO> success = new ArrayList<>();
        List<WorkScheduleDTO> failure = new ArrayList<>();

        for (WorkScheduleCommand slot : slots) {
            if(scheduleValidator.isScheduleInsertable(slot)){
                User user = userRepository.findByEmail(slot.email())
                        .orElseThrow(() -> UserNotFoundException.of(
                                GlobalErrorCode.USER_NOT_FOUND, UserNotFoundResponseDetail.of(slot.email())));
                workSchedulesRepository.save(WorkScheduleCommand.toEntity(slot, user));
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
}
