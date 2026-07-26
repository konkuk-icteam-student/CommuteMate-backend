package com.better.CommuteMate.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "work_schedule_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_wss_org_year_month",
                        columnNames = {"organization_id", "year", "month"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkScheduleSetting {

    @Id
    @Column(name = "setting_id", nullable = false, length = 36)
    private String settingId;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "apply_start_at", nullable = false)
    private LocalDateTime applyStartAt;

    @Column(name = "apply_end_at", nullable = false)
    private LocalDateTime applyEndAt;

    @Column(name = "apply_enabled", nullable = false)
    @Builder.Default
    private Boolean applyEnabled = true;

    @Column(name = "edit_enabled", nullable = false)
    @Builder.Default
    private Boolean editEnabled = true;

    @Column(name = "auto_approve_enabled", nullable = false)
    @Builder.Default
    private Boolean autoApproveEnabled = false;

    @Column(name = "max_concurrent_workers", nullable = false)
    private Integer maxConcurrentWorkers;

    @Column(name = "min_work_unit_minutes", nullable = false)
    @Builder.Default
    private Integer minWorkUnitMinutes = 30;

    @Column(name = "max_work_unit_minutes")
    private Integer maxWorkUnitMinutes;

    @Column(name = "monthly_required_minutes", nullable = false)
    private Integer monthlyRequiredMinutes;

    @Column(name = "monthly_min_minutes")
    private Integer monthlyMinMinutes;

    @Column(name = "monthly_max_minutes")
    private Integer monthlyMaxMinutes;

    @Column(name = "weekly_min_minutes")
    private Integer weeklyMinMinutes;

    @Column(name = "weekly_max_minutes")
    private Integer weeklyMaxMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public void updateApplyPeriod(
            LocalDateTime applyStartAt,
            LocalDateTime applyEndAt,
            String updatedBy
    ) {
        this.applyStartAt = applyStartAt;
        this.applyEndAt = applyEndAt;
        this.updatedBy = updatedBy;
    }

    public void updateEnabledStatus(
            Boolean applyEnabled,
            Boolean editEnabled,
            Boolean autoApproveEnabled,
            String updatedBy
    ) {
        this.applyEnabled = applyEnabled;
        this.editEnabled = editEnabled;
        this.autoApproveEnabled = autoApproveEnabled;
        this.updatedBy = updatedBy;
    }

    public void updateWorkRule(
            Integer maxConcurrentWorkers,
            Integer minWorkUnitMinutes,
            Integer maxWorkUnitMinutes,
            Integer monthlyRequiredMinutes,
            Integer weeklyMinMinutes,
            Integer weeklyMaxMinutes,
            String updatedBy
    ) {
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.minWorkUnitMinutes = minWorkUnitMinutes;
        this.maxWorkUnitMinutes = maxWorkUnitMinutes;
        this.monthlyRequiredMinutes = monthlyRequiredMinutes;
        this.weeklyMinMinutes = weeklyMinMinutes;
        this.weeklyMaxMinutes = weeklyMaxMinutes;
        this.updatedBy = updatedBy;
    }

    public void updateMonthlySetting(
            LocalDateTime applyStartAt,
            LocalDateTime applyEndAt,
            Integer maxConcurrentWorkers,
            Integer minWorkUnitMinutes,
            Integer weeklyMinMinutes,
            Integer weeklyMaxMinutes,
            Integer monthlyMinMinutes,
            Integer monthlyMaxMinutes,
            String updatedBy
    ) {
        this.applyStartAt = applyStartAt;
        this.applyEndAt = applyEndAt;
        this.maxConcurrentWorkers = maxConcurrentWorkers;
        this.minWorkUnitMinutes = minWorkUnitMinutes;
        this.weeklyMinMinutes = weeklyMinMinutes;
        this.weeklyMaxMinutes = weeklyMaxMinutes;
        this.monthlyMinMinutes = monthlyMinMinutes;
        this.monthlyMaxMinutes = monthlyMaxMinutes;
        // Keep the legacy limit used by the student schedule APIs in sync.
        this.monthlyRequiredMinutes = monthlyMaxMinutes;
        this.updatedBy = updatedBy;
    }

    public boolean isApplyPeriod(LocalDateTime now) {
        return applyEnabled
                && !now.isBefore(applyStartAt)
                && !now.isAfter(applyEndAt);
    }

    @PrePersist
    protected void onCreate() {
        if (this.settingId == null) {
            this.settingId = UUID.randomUUID().toString();
        }

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
