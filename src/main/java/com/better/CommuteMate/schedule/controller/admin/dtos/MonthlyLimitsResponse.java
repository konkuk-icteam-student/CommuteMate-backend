package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleLimit;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MonthlyLimitsResponse extends ResponseDetail {
    private final List<MonthlyLimitItem> limits;

    @Getter
    @Builder
    public static class MonthlyLimitItem {
        private final Integer year;
        private final Integer month;
        private final Integer maxConcurrent;

        public static MonthlyLimitItem from(MonthlyScheduleLimit limit) {
            return MonthlyLimitItem.builder()
                    .year(limit.getScheduleYear())
                    .month(limit.getScheduleMonth())
                    .maxConcurrent(limit.getMaxConcurrent())
                    .build();
        }
    }

    public static MonthlyLimitsResponse from(List<MonthlyScheduleLimit> limits) {
        return MonthlyLimitsResponse.builder()
                .limits(limits.stream().map(MonthlyLimitItem::from).toList())
                .build();
    }
}