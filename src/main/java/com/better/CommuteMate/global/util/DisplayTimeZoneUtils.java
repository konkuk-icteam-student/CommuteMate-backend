package com.better.CommuteMate.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// [임시] 전역 타임존(UTC) 미해결로 인한 출력 KST 보정.
// 전역 타임존 KST 전환 시 이 유틸과 모든 호출처를 제거할 것(이중 보정 방지).
public class DisplayTimeZoneUtils {

    private static final long KST_OFFSET_HOURS = 9;

    private DisplayTimeZoneUtils() {}

    // [임시] 전역 타임존(UTC) 미해결로 인한 출력 KST 보정.
    // 전역 타임존 KST 전환 시 이 유틸과 모든 호출처를 제거할 것(이중 보정 방지).
    public static LocalDateTime toKstForDisplay(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.plusHours(KST_OFFSET_HOURS);
    }

    // [임시] 전역 타임존(UTC) 미해결로 인한 출력 KST 보정.
    // 전역 타임존 KST 전환 시 이 유틸과 모든 호출처를 제거할 것(이중 보정 방지).
    // date/time을 합쳐 +9시간 한 뒤 시각만 반환한다(자정 넘김을 date와 합쳐 정확히 처리하기 위함).
    public static LocalTime toKstTimeForDisplay(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return date.atTime(time).plusHours(KST_OFFSET_HOURS).toLocalTime();
    }
}
