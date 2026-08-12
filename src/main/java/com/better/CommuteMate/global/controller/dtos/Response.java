package com.better.CommuteMate.global.controller.dtos;

public record Response<T>(
    boolean isSuccess,
    String message,
    T details
) {
    public static Response of(boolean isSuccess, String message, ResponseDetail details) {
        return new Response(isSuccess, message, details);
    }

}
