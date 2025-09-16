package com.better.CommuteMate.global.controller.dtos;

public record Response(
    boolean isSuccess,
    String description,
    ResponseDetail details
) {

}
