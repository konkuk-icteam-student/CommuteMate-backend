package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminUserSearchResponse extends ResponseDetail {

    public final List<UserSummary> users;

    public AdminUserSearchResponse(List<UserSummary> users) {
        this.users = users;
    }

    public static AdminUserSearchResponse from(List<User> users, Map<Long, UserProfile> profiles) {
        return new AdminUserSearchResponse(users.stream()
                .map(user -> {
                    UserProfile profile = profiles.get(user.getUserId());
                    return new UserSummary(
                            user.getUserId(),
                            user.getName(),
                            profile == null ? null : profile.getDepartment(),
                            profile == null ? null : profile.getStudentId()
                    );
                })
                .toList());
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record UserSummary(Long userId, String userName, String department, String studentId) {
    }
}
