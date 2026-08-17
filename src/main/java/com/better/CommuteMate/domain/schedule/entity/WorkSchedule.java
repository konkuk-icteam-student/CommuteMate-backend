package com.better.CommuteMate.domain.schedule.entity;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "work_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id", nullable = false)
    private WorkScheduleSetting setting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", nullable = false)
    private Workplace workplace;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "work_status_code", columnDefinition = "CHAR(4)")
    private CodeType workStatusCode = CodeType.WK01;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status_code", columnDefinition = "CHAR(4)")
    private CodeType attendanceStatusCode;

    @Column(name = "created_request_id", length = 36)
    private String createdRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public void updateSchedule(
            WorkScheduleSetting setting,
            Workplace workplace,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String updatedBy
    ) {
        this.setting = setting;
        this.workplace = workplace;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedBy = updatedBy;
    }

    public void updateStatus(CodeType statusCode, String updatedBy) {
        this.statusCode = statusCode;
        this.updatedBy = updatedBy;
    }

    public void approveChangeRequest(String adminId, CodeType requestTypeCode) {
        this.updatedBy = adminId;

        if (requestTypeCode.equals(CodeType.CR01)) {
            this.statusCode = CodeType.WS02;
        } else if (requestTypeCode.equals(CodeType.CR02)) {
            this.statusCode = CodeType.WS04;
        }
    }

    public void cancel(String updatedBy) {
        this.statusCode = CodeType.WS04;
        this.updatedBy = updatedBy;
    }

    public void markWorking(boolean late, String updatedBy) {
        this.workStatusCode = CodeType.WK02;
        this.attendanceStatusCode = late ? CodeType.AT02 : CodeType.AT01;
        this.updatedBy = updatedBy;
    }

    public void markCompleted(String updatedBy) {
        this.workStatusCode = CodeType.WK03;
        this.updatedBy = updatedBy;
    }

    public void markNoShow(boolean absent, String updatedBy) {
        this.workStatusCode = CodeType.WK04;
        this.attendanceStatusCode = absent ? CodeType.AT03 : null;
        this.updatedBy = updatedBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
