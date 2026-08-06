package com.better.CommuteMate.schedule.controller.admin.dtos;

import java.util.List;

public record BulkApproveWorkChangeRequest(
        List<Long> requestIds
) {
}
