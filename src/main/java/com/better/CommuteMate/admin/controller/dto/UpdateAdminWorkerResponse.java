package com.better.CommuteMate.admin.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class UpdateAdminWorkerResponse extends ResponseDetail {
    public final Long userId;
    public final String name;
    public final String studentId;
    public final String department;
    public final Integer grade;
    public final String phoneNumber;
    public final String email;
    @JsonFormat(pattern = "yyyy-MM-dd") public final LocalDate workStartDate;

    public UpdateAdminWorkerResponse(User user, UserProfile profile) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.studentId = profile.getStudentId();
        this.department = profile.getDepartment();
        this.grade = profile.getGrade();
        this.phoneNumber = profile.getPhoneNumber();
        this.email = user.getEmail();
        this.workStartDate = user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate();
    }
}
