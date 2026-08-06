package com.better.CommuteMate.schedule.controller.schedule.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 근무 일정 변경 요청 DTO
 * 변경사항만 addSlots / deleteSlots로 전달
 */
public record WorkScheduleChangeRequest(
        List<Slot> addSlots,
        List<Slot> deleteSlots
) {

    /**
     * 추가할 일정 목록이 null인 경우 빈 리스트 반환
     */
    public List<Slot> addSlotsOrEmpty() {
        return addSlots == null ? List.of() : addSlots;
    }

    /**
     * 삭제할 일정 목록이 null인 경우 빈 리스트 반환
     */
    public List<Slot> deleteSlotsOrEmpty() {
        return deleteSlots == null ? List.of() : deleteSlots;
    }

    /**
     * 근무 일정 변경 요청의 단일 시간 슬롯
     * 날짜 형식: yyyy-MM-dd
     * 시간 형식: HH:mm
     */
    public record Slot(
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {

        /**
         * 응답 반환용 시작 일시 생성
         */
        public LocalDateTime startDateTime() {
            return LocalDateTime.of(date, start);
        }

        /**
         * 응답 반환용 종료 일시 생성
         */
        public LocalDateTime endDateTime() {
            return LocalDateTime.of(date, end);
        }
    }
}