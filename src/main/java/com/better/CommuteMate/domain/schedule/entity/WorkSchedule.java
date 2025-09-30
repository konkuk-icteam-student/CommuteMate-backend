package com.better.CommuteMate.domain.schedule.entity;

import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSchedule {

    @Id
    @Column(name = "schedule_id", columnDefinition = "CHAR(36)", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private WorkScheduleStatusCode statusCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private String updatedBy;

    @Builder
    public WorkSchedule(
            User user,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.user = user;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.statusCode = WorkScheduleStatusCode.WS02;
        this.createdBy = LocalDateTime.now().toString();
        this.updatedBy = LocalDateTime.now().toString();
    }

}