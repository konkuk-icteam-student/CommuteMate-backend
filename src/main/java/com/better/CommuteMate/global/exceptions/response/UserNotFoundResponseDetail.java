package com.better.CommuteMate.global.exceptions.response;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;

@Builder
public class UserNotFoundResponseDetail extends ErrorResponseDetail {
    String userEmail;
    public static UserNotFoundResponseDetail of(String userEmail) {
        return UserNotFoundResponseDetail.builder()
                .userEmail(userEmail)
                .build();
    }
}
