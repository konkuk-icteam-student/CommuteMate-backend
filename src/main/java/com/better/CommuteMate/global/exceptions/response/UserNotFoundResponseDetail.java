package com.better.CommuteMate.global.exceptions.response;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserNotFoundResponseDetail extends ErrorResponseDetail {
    Long userID;
    public static UserNotFoundResponseDetail of(Long userId) {
        return UserNotFoundResponseDetail.builder()
                .userID(userId)
                .build();
    }
}
