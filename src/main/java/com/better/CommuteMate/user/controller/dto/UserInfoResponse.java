package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserInfoResponse extends ResponseDetail {
    private final Integer userId;
    private final String email;
    private final String name;
    private final CodeType role;
    private final Integer organizationId;

    @Builder
    public UserInfoResponse(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRoleCode();
        this.organizationId = user.getOrganizationId();
    }
}
