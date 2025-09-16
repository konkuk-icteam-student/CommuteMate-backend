package com.better.CommuteMate.global.controller.dtos;

import java.time.LocalDateTime;

public abstract class ResponseDetail {
    protected String codeId;
    protected String codeName;
    protected String codeValue;
    protected LocalDateTime timestamp;

    public ResponseDetail(String codeId, String codeName, String codeValue) {
        this.codeId = codeId;
        this.codeName = codeName;
        this.codeValue = codeValue;
        this.timestamp = LocalDateTime.now();
    }
}
