package com.better.CommuteMate.global.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 주차 계산 유틸리티.
 * 규칙: 해당 월 1일이 속한 주를 1주차로 본다. 주의 시작 요일은 월요일.
 */
public class WorkWeekUtils {

    private WorkWeekUtils() {}

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static int weekOfMonth(LocalDate date) {
        LocalDate weekOneStart = weekStart(date.withDayOfMonth(1));
        long weeks = ChronoUnit.WEEKS.between(weekOneStart, weekStart(date));
        return (int) weeks + 1;
    }

    public static boolean isSameWeek(LocalDate a, LocalDate b) {
        return weekStart(a).equals(weekStart(b));
    }
}
