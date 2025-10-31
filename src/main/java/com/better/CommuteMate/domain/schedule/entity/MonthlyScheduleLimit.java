package com.better.CommuteMate.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_schedule_limit",
       uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_year", "schedule_month"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MonthlyScheduleLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id", nullable = false)
    private Integer limitId;

    @Column(name = "schedule_year", nullable = false)
    private Integer scheduleYear;

    @Column(name = "schedule_month", nullable = false)
    private Integer scheduleMonth;

    @Column(name = "max_concurrent", nullable = false)
    private Integer maxConcurrent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Integer createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}