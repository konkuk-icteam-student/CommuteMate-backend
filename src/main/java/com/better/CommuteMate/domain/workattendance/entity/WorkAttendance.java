package com.better.CommuteMate.domain.workattendance.entity;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAttendance {

    @Id
    @Column(name = "attendance_id", length = 36, nullable = false)
    private String attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Column(name = "check_time", nullable = false)
    private Instant checkTime;

    @Column(name = "check_type_code", columnDefinition = "CHAR(4)", nullable = false)
    private String checkTypeCode;

    @Builder.Default
    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}