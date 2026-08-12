package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkSlotUtilsTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    private WorkScheduleSlotCommand slot(String start, String end) {
        return new WorkScheduleSlotCommand(DATE, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    @DisplayName("단일 슬롯은 그대로 하나의 구간으로 반환된다")
    void mergeConsecutiveRanges_SingleSlot_ReturnsSelf() {
        List<WorkSlotUtils.TimeRange> result =
                WorkSlotUtils.mergeConsecutiveRanges(List.of(slot("09:00", "10:30")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).end()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("인접한 두 슬롯은 하나의 연속 구간으로 병합된다")
    void mergeConsecutiveRanges_TwoAdjacentSlots_MergesIntoOne() {
        List<WorkSlotUtils.TimeRange> result = WorkSlotUtils.mergeConsecutiveRanges(
                List.of(slot("09:00", "09:30"), slot("09:30", "10:30")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).end()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("인접한 세 슬롯은 하나의 연속 구간으로 병합된다")
    void mergeConsecutiveRanges_ThreeConsecutiveSlots_MergesIntoOne() {
        List<WorkSlotUtils.TimeRange> result = WorkSlotUtils.mergeConsecutiveRanges(
                List.of(slot("09:00", "09:30"), slot("09:30", "10:00"), slot("10:00", "11:00")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).end()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    @DisplayName("끊긴 두 슬롯은 각각 별도 구간으로 반환된다")
    void mergeConsecutiveRanges_TwoDisjointSlots_ReturnsTwoRanges() {
        List<WorkSlotUtils.TimeRange> result = WorkSlotUtils.mergeConsecutiveRanges(
                List.of(slot("09:00", "09:30"), slot("10:00", "10:30")));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).end()).isEqualTo(LocalTime.of(9, 30));
        assertThat(result.get(1).start()).isEqualTo(LocalTime.of(10, 0));
        assertThat(result.get(1).end()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("빈 목록은 빈 결과를 반환한다")
    void mergeConsecutiveRanges_EmptyList_ReturnsEmpty() {
        assertThat(WorkSlotUtils.mergeConsecutiveRanges(List.of())).isEmpty();
    }

    @Test
    @DisplayName("입력 순서가 뒤바뀌어도 정렬 후 올바르게 병합된다")
    void mergeConsecutiveRanges_UnorderedInput_SortsAndMerges() {
        List<WorkSlotUtils.TimeRange> result = WorkSlotUtils.mergeConsecutiveRanges(
                List.of(slot("09:30", "10:00"), slot("09:00", "09:30")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).end()).isEqualTo(LocalTime.of(10, 0));
    }
}
