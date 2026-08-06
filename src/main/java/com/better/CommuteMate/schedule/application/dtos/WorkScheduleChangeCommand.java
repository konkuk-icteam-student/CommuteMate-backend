package com.better.CommuteMate.schedule.application.dtos;

import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeRequest;

import java.util.List;

/**
 * 근무 일정 변경 요청 Command
 * 추가할 일정(addSlots)과 삭제할 일정(deleteSlots)을 함께 전달
 */
public record WorkScheduleChangeCommand(
        Long userId,
        List<WorkScheduleSlotCommand> addSlots,
        List<WorkScheduleSlotCommand> deleteSlots
) {

    /**
     * Controller Request DTO를 Application Command로 변환
     */
    public static WorkScheduleChangeCommand from(
            WorkScheduleChangeRequest request,
            Long userId
    ) {
        return new WorkScheduleChangeCommand(
                userId,
                request.addSlotsOrEmpty().stream()
                        .map(slot -> new WorkScheduleSlotCommand(
                                slot.date(),
                                slot.start(),
                                slot.end()
                        ))
                        .toList(),
                request.deleteSlotsOrEmpty().stream()
                        .map(slot -> new WorkScheduleSlotCommand(
                                slot.date(),
                                slot.start(),
                                slot.end()
                        ))
                        .toList()
        );
    }

    /**
     * 추가 일정 목록이 비어있는지 확인
     */
    public boolean hasNoAddSlots() {
        return addSlots == null || addSlots.isEmpty();
    }

    /**
     * 삭제 일정 목록이 비어있는지 확인
     */
    public boolean hasNoDeleteSlots() {
        return deleteSlots == null || deleteSlots.isEmpty();
    }

    /**
     * 변경 요청이 비어있는지 확인
     */
    public boolean isEmpty() {
        return hasNoAddSlots() && hasNoDeleteSlots();
    }
}