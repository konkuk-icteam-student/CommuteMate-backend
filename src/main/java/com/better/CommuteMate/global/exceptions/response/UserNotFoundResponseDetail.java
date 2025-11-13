package com.better.CommuteMate.global.exceptions.response;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserNotFoundResponseDetail extends ErrorResponseDetail {
    Integer userID;
    public static UserNotFoundResponseDetail of(Integer userId) {
        return UserNotFoundResponseDetail.builder()
                .userID(userId)
                .build();
    }
}
