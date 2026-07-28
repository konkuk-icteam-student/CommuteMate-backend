package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

public class BulkApproveWorkChangeResponse extends ResponseDetail {
    public final Summary summary;
    public final List<Result> results;

    public BulkApproveWorkChangeResponse(Summary summary, List<Result> results) {
        this.summary = summary;
        this.results = results;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record Summary(
            int totalCount,
            int successCount,
            int failCount
    ) {
    }

    public record Result(
            Long requestId,
            String resultCode,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime processedAt
    ) {
    }
}
