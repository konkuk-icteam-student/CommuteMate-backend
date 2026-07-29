package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

public class AdminUserSearchResponse extends ResponseDetail {

    public final List<UserSummary> users;

    public AdminUserSearchResponse(List<UserSummary> users) {
        this.users = users;
    }

    public static AdminUserSearchResponse from(List<User> users) {
        return new AdminUserSearchResponse(users.stream()
                .map(user -> new UserSummary(
                        String.valueOf(user.getUserId()),
                        user.getName()
                ))
                .toList());
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record UserSummary(String userId, String userName) {
    }
}
