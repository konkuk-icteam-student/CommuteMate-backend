package com.better.CommuteMate.global.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayTimeZoneUtilsTest {

    @Test
    void toKstForDisplay_addsNineHours() {
        LocalDateTime utc = LocalDateTime.of(2026, 4, 15, 10, 36);

        assertThat(DisplayTimeZoneUtils.toKstForDisplay(utc))
                .isEqualTo(LocalDateTime.of(2026, 4, 15, 19, 36));
    }

    @Test
    void toKstForDisplay_nullInputReturnsNull() {
        assertThat(DisplayTimeZoneUtils.toKstForDisplay(null)).isNull();
    }

    @Test
    void toKstTimeForDisplay_addsNineHoursAndExtractsTimeOnly() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        LocalTime time = LocalTime.of(9, 13);

        assertThat(DisplayTimeZoneUtils.toKstTimeForDisplay(date, time))
                .isEqualTo(LocalTime.of(18, 13));
    }

    @Test
    void toKstTimeForDisplay_handlesMidnightCrossing() {
        // UTC 23:00 (completion_date=D) + 9h = KST D+1 08:00 -> 시각만 추출하면 08:00
        LocalDate date = LocalDate.of(2026, 4, 15);
        LocalTime time = LocalTime.of(23, 0);

        assertThat(DisplayTimeZoneUtils.toKstTimeForDisplay(date, time))
                .isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void toKstTimeForDisplay_nullDateReturnsNull() {
        assertThat(DisplayTimeZoneUtils.toKstTimeForDisplay(null, LocalTime.of(9, 0))).isNull();
    }

    @Test
    void toKstTimeForDisplay_nullTimeReturnsNull() {
        assertThat(DisplayTimeZoneUtils.toKstTimeForDisplay(LocalDate.of(2026, 4, 15), null)).isNull();
    }
}
