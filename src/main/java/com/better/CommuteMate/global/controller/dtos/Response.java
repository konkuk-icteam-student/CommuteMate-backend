package com.better.CommuteMate.global.controller.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

public record Response(
    boolean isSuccess,
    @JsonInclude(JsonInclude.Include.NON_NULL) String code,
    String message,
    ResponseDetail details
) {
    public Response(boolean isSuccess, String message, ResponseDetail details) {
        this(isSuccess, null, message, details);
    }

    public static Response of(boolean isSuccess, String message, ResponseDetail details) {
        return new Response(isSuccess, null, message, details);
    }

    public static Response error(String code, String message) {
        return new Response(false, code, message, null);
    }
}
