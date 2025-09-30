package com.better.CommuteMate.global.controller.dtos;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class ResponseDetail {
    protected LocalDateTime timestamp;

    public ResponseDetail() {
        this.timestamp = LocalDateTime.now();
    }
}
