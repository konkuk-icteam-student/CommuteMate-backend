package com.better.CommuteMate.global.util;

import java.time.LocalDateTime;

public final class AttendancePolicy {
    private AttendancePolicy() {}

    public static boolean isLate(LocalDateTime scheduledStart, LocalDateTime checkIn) {
        return !checkIn.isBefore(scheduledStart.plusMinutes(5));
    }
}
