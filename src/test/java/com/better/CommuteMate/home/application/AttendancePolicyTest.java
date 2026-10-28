package com.better.CommuteMate.home.application;

import com.better.CommuteMate.global.util.AttendancePolicy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AttendancePolicyTest {
    @Test
    void lateStartsAtExactlyFiveMinutes() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0);
        assertThat(AttendancePolicy.isLate(start, start.plusMinutes(5).minusNanos(1))).isFalse();
        assertThat(AttendancePolicy.isLate(start, start.plusMinutes(5))).isTrue();
        assertThat(AttendancePolicy.isLate(start, start.plusMinutes(25))).isTrue();
    }
}
