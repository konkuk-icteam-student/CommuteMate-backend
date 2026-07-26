package com.better.CommuteMate.schedule.controller.schedule.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WorkScheduleEditRequest(
        List<Slot> deleteSlots,
        List<Slot> addSlots,
        String reason
) {

    public List<Slot> deleteSlotsOrEmpty() {
        return deleteSlots == null ? List.of() : deleteSlots;
    }

    public List<Slot> addSlotsOrEmpty() {
        return addSlots == null ? List.of() : addSlots;
    }

    public record Slot(
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {}
}
