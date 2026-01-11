package com.better.CommuteMate.schedule.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type; // e.g., "SCHEDULE_APPROVED", "SCHEDULE_REJECTED"
    private String message;
    private Object data; // Optional data payload
}
