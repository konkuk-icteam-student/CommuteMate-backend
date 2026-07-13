package com.better.CommuteMate.schedule.controller.schedule.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 근무 일정 변경 응답 상세 DTO
 * 성공한 일정과 실패한 일정을 분리하여 반환
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkScheduleChangeResponseDetail extends ResponseDetail {

    private final List<Slot> success;

    /**
     * 완전 성공 응답에서 사용하는 실패 목록 필드
     * 명세상 완전 성공에서는 fail: [] 로 반환
     */
    private final List<Slot> fail;

    /**
     * 일부 성공 / 전부 실패 응답에서 사용하는 실패 목록 필드
     * 명세상 실패가 존재하는 경우 failure 로 반환
     */
    private final List<Slot> failure;

    /**
     * 완전 성공 응답 상세 생성
     */
    public static WorkScheduleChangeResponseDetail allSuccess(List<Slot> success) {
        return WorkScheduleChangeResponseDetail.builder()
                .success(success)
                .fail(List.of())
                .failure(null)
                .build();
    }

    /**
     * 일부 성공 또는 전부 실패 응답 상세 생성
     */
    public static WorkScheduleChangeResponseDetail withFailure(
            List<Slot> success,
            List<Slot> failure
    ) {
        return WorkScheduleChangeResponseDetail.builder()
                .success(success)
                .fail(null)
                .failure(failure)
                .build();
    }

    /**
     * 요청 슬롯을 응답 슬롯으로 변환
     */
    public static Slot from(WorkScheduleChangeRequest.Slot slot) {
        return new Slot(
                slot.startDateTime(),
                slot.endDateTime()
        );
    }

    /**
     * 근무 일정 변경 결과의 단일 시간 슬롯 응답
     * start, end를 LocalDateTime 형식으로 반환
     */
    public record Slot(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}