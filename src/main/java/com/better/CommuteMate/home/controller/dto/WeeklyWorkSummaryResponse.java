package com.better.CommuteMate.home.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주간/월간 근무 시간 요약 응답 DTO
 * <p>
 * - totalWeeklyHours: 이번 주 전체 근무 시간 (승인된 스케줄 기준)
 * - completedWeeklyHours: 이번 주 완료된 근무 시간 (퇴근 체크된 스케줄만)
 * - completedMonthlyHours: 이번 달 완료된 근무 시간 (퇴근 체크된 스케줄만)
 * </p>
 */
@Getter
@NoArgsConstructor
public class WeeklyWorkSummaryResponse extends ResponseDetail {

    /**
     * 이번 주 전체 근무 시간 (시간 단위, 0.5 = 30분)
     * 예: 3.0, 3.5, 4.0
     */
    private Double totalWeeklyHours;

    /**
     * 이번 주 완료된 근무 시간 (시간 단위, 0.5 = 30분)
     * 퇴근 체크가 완료된 스케줄만 포함
     */
    private Double completedWeeklyHours;

    /**
     * 이번 달 완료된 근무 시간 (시간 단위, 0.5 = 30분)
     * 퇴근 체크가 완료된 스케줄만 포함
     */
    private Double completedMonthlyHours;

    @Builder
    public WeeklyWorkSummaryResponse(Double totalWeeklyHours, Double completedWeeklyHours, Double completedMonthlyHours) {
        super();
        this.totalWeeklyHours = totalWeeklyHours;
        this.completedWeeklyHours = completedWeeklyHours;
        this.completedMonthlyHours = completedMonthlyHours;
    }
}
