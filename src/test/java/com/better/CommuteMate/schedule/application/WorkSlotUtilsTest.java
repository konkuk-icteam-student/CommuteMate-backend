package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.schedule.application.WorkSlotUtils.SlotKey;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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

    // ─── isAllDayUnavailable ──────────────────────────────────────────────────

    @Test
    @DisplayName("종일 불가(MIN~MAX) 저장값은 isAllDayUnavailable=true 반환")
    void isAllDayUnavailable_MinToMax_ReturnsTrue() {
        WorkUnavailableTime allDay = WorkUnavailableTime.builder()
                .date(DATE).startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        assertThat(WorkSlotUtils.isAllDayUnavailable(allDay)).isTrue();
    }

    @Test
    @DisplayName("부분 불가(10:00~12:00)는 isAllDayUnavailable=false 반환")
    void isAllDayUnavailable_PartialTime_ReturnsFalse() {
        WorkUnavailableTime partial = WorkUnavailableTime.builder()
                .date(DATE).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0)).build();

        assertThat(WorkSlotUtils.isAllDayUnavailable(partial)).isFalse();
    }

    @Test
    @DisplayName("이전 버그 조건(MIN~MIN)은 isAllDayUnavailable=false — sentinel 오판정 회귀 방지")
    void isAllDayUnavailable_MinToMin_ReturnsFalse() {
        WorkUnavailableTime minToMin = WorkUnavailableTime.builder()
                .date(DATE).startTime(LocalTime.MIN).endTime(LocalTime.MIN).build();

        assertThat(WorkSlotUtils.isAllDayUnavailable(minToMin)).isFalse();
    }

    // ─── expandToSlots ────────────────────────────────────────────────────────

    @Test
    @DisplayName("09:00~11:00을 30분 단위로 분할하면 4개 슬롯")
    void expandToSlots_PartialRange_ReturnsFourSlots() {
        List<SlotKey> slots = WorkSlotUtils.expandToSlots(
                DATE, LocalTime.of(9, 0), LocalTime.of(11, 0), 30);

        assertThat(slots).hasSize(4);
        assertThat(slots.get(0)).isEqualTo(new SlotKey(DATE, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        assertThat(slots.get(3)).isEqualTo(new SlotKey(DATE, LocalTime.of(10, 30), LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("endTime=LocalTime.MAX(23:59:59...)는 자정 wrap-around 없이 안전 캡 이내 슬롯 반환")
    void expandToSlots_LocalTimeMax_NoBoundaryOverflow() {
        List<SlotKey> slots = WorkSlotUtils.expandToSlots(
                DATE, LocalTime.MIN, LocalTime.MAX, 30);

        // 00:00~24:00 = 48 슬롯, 안전 캡 48개 이내
        assertThat(slots).hasSize(48);
        assertThat(slots.get(0)).isEqualTo(new SlotKey(DATE, LocalTime.of(0, 0), LocalTime.of(0, 30)));
        assertThat(slots.get(47)).isEqualTo(new SlotKey(DATE, LocalTime.of(23, 30), LocalTime.of(0, 0)));
    }

    // ─── buildUnavailableSlotKeys ─────────────────────────────────────────────

    @Test
    @DisplayName("종일 불가(MIN~MAX)는 09:00~18:00의 30분 슬롯 전부(18개)로 확장")
    void buildUnavailableSlotKeys_AllDay_Returns18SlotsFrom09To18() {
        WorkUnavailableTime allDay = WorkUnavailableTime.builder()
                .date(DATE).startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        Set<SlotKey> result = WorkSlotUtils.buildUnavailableSlotKeys(
                List.of(allDay), LocalTime.of(9, 0), LocalTime.of(18, 0), 30);

        assertThat(result).hasSize(18);
        assertThat(result).contains(new SlotKey(DATE, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        assertThat(result).contains(new SlotKey(DATE, LocalTime.of(17, 30), LocalTime.of(18, 0)));
        assertThat(result).doesNotContain(new SlotKey(DATE, LocalTime.of(8, 30), LocalTime.of(9, 0)));
        assertThat(result).doesNotContain(new SlotKey(DATE, LocalTime.of(18, 0), LocalTime.of(18, 30)));
    }

    @Test
    @DisplayName("부분 불가(10:00~12:00)는 해당 범위 슬롯(4개)만 포함")
    void buildUnavailableSlotKeys_PartialTime_ReturnsOnlyThatRange() {
        WorkUnavailableTime partial = WorkUnavailableTime.builder()
                .date(DATE).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0)).build();

        Set<SlotKey> result = WorkSlotUtils.buildUnavailableSlotKeys(
                List.of(partial), LocalTime.of(9, 0), LocalTime.of(18, 0), 30);

        assertThat(result).hasSize(4);
        assertThat(result).contains(new SlotKey(DATE, LocalTime.of(10, 0), LocalTime.of(10, 30)));
        assertThat(result).contains(new SlotKey(DATE, LocalTime.of(11, 30), LocalTime.of(12, 0)));
        assertThat(result).doesNotContain(new SlotKey(DATE, LocalTime.of(9, 30), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("빈 목록은 빈 Set 반환")
    void buildUnavailableSlotKeys_EmptyList_ReturnsEmpty() {
        Set<SlotKey> result = WorkSlotUtils.buildUnavailableSlotKeys(
                List.of(), LocalTime.of(9, 0), LocalTime.of(18, 0), 30);

        assertThat(result).isEmpty();
    }
}
