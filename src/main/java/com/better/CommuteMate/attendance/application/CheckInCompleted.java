package com.better.CommuteMate.attendance.application;

import java.time.LocalDateTime;

public record CheckInCompleted(Long organizationId, String studentName, LocalDateTime checkTime) {
}
