package com.better.CommuteMate.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "student_id", length = 30)
    private String studentId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    public void update(String studentId, String department, Integer grade, String phoneNumber) {
        if (studentId != null) this.studentId = studentId;
        if (department != null) this.department = department;
        if (grade != null) this.grade = grade;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
    }
}
