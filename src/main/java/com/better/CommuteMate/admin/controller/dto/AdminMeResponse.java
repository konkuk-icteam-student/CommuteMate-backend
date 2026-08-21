package com.better.CommuteMate.admin.controller.dto;

import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class AdminMeResponse extends ResponseDetail {
    public final Long userId;
    public final String adminName;
    public final String teamName;

    private AdminMeResponse(User admin, Organization organization) {
        this.userId = admin.getUserId();
        this.adminName = admin.getName();
        this.teamName = organization.getName();
    }

    public static AdminMeResponse of(User admin, Organization organization) {
        return new AdminMeResponse(admin, organization);
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
