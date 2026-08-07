package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class SystemCreatedYearResponse extends ResponseDetail {

    @Schema(description = "시스템(조직) 생성 연도", example = "2024")
    public final int createdYear;

    public SystemCreatedYearResponse(int createdYear) {
        this.createdYear = createdYear;
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
