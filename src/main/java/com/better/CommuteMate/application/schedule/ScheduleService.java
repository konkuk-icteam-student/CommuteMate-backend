package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.application.schedule.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.application.schedule.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.application.schedule.exceptions.SchedulePartialFailureException;
import com.better.CommuteMate.application.schedule.exceptions.response.ScheduleResponseDetail;
import com.better.CommuteMate.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.domain.schedule.command.ApplyScheduleResultCommand;
import com.better.CommuteMate.domain.schedule.command.ScheduleCommand;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkSchedulesRepository workSchedulesRepository;


    public ApplyScheduleResultCommand applyWorkSchedules(List<ScheduleCommand> slots) {
        List<WorkSchedule> success = new ArrayList<>();
        List<WorkSchedule> failure = new ArrayList<>();

        for (ScheduleCommand slot : slots) {
            try {
                //ScheduleCommand saved = workSchedulesRepository.save(slot);
                //success.add(saved);
            } catch (Exception e) {
                //fail.add(slot);
            }
        }
        // 이런 형식으로, 성공/실패에 대한 내용을 담아서 오류 반환->GlobalExceptionHandler에서 처리
        if(success.isEmpty()){
            throw ScheduleAllFailureException.of(ScheduleErrorCode.SCHEDULE_FAILURE, ScheduleResponseDetail.of(success,failure));
        }else if(!failure.isEmpty()){
            throw SchedulePartialFailureException.of(ScheduleErrorCode.SCHEDULE_PARTIAL_FAILURE, ScheduleResponseDetail.of(success,failure));
        }
        // 오류 발생 안하는 경우->전부 성공한 경우
        return null;
//        return new ApplyScheduleResultCommand(
//            success.stream().map(slot -> new WorkSchedule(slot.start(), slot.end())).toList(),
//            fail.stream().map(slot -> new WorkSchedule(slot.start(), slot.end())).toList()
//        );
    }
}
