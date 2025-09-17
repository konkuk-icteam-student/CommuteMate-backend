package com.better.CommuteMate.global.controller.dtos;

import java.time.LocalDateTime;

public abstract class ResponseDetail {
    protected LocalDateTime timestamp;

    public ResponseDetail() {
        this.timestamp = LocalDateTime.now();
    }
}
