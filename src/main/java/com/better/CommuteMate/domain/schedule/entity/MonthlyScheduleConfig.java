package com.better.CommuteMate.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_schedule_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_year", "schedule_month"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MonthlyScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id", nullable = false)
    private Long limitId;

    @Column(name = "schedule_year", nullable = false)
    private Integer scheduleYear;

    @Column(name = "schedule_month", nullable = false)
    private Integer scheduleMonth;

    @Column(name = "max_concurrent", nullable = false)
    private Integer maxConcurrent;

    @Column(name = "apply_start_time", nullable = false)
    private LocalDateTime applyStartTime;

    @Column(name = "apply_end_time", nullable = false)
    private LocalDateTime applyEndTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

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