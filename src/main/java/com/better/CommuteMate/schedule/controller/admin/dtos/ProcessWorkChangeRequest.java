package com.better.CommuteMate.schedule.controller.admin.dtos;

public record ProcessWorkChangeRequest(
        String statusCode,
        String rejectReason
) {
}
