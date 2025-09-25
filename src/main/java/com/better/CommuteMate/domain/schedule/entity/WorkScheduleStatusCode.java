package com.better.CommuteMate.domain.schedule.entity;

import lombok.Getter;

@Getter
public enum WorkScheduleStatusCode {
    WS01("REQUESTED", "신청"),
    WS02("APPROVED", "승인"),
    WS03("REJECTED", "반려");

    private final String codeName;
    private final String codeValue; // 화면 표시용

    WorkScheduleStatusCode(String codeName, String codeValue) {
        this.codeName = codeName;
        this.codeValue = codeValue;
    }

    public String getCode() { return this.name(); }
}