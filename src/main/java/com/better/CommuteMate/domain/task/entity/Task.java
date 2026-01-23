package com.better.CommuteMate.domain.task.entity;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "task", indexes = {
        @Index(name = "idx_task_date", columnList = "task_date"),
        @Index(name = "idx_task_assignee", columnList = "assignee_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(name = "task_time", nullable = false)
    private LocalTime taskTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType taskType;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_by_name", length = 50)
    private String completedByName;

    @Column(name = "completed_time")
    private LocalTime completedTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
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

    // 업무 정보 수정
    public void update(String title, User assignee, LocalTime taskTime, Integer updatedBy) {
        if (title != null) {
            this.title = title;
        }
        if (assignee != null) {
            this.assignee = assignee;
        }
        if (taskTime != null) {
            this.taskTime = taskTime;
        }
        this.updatedBy = updatedBy;
    }

    // 완료 상태 토글
    public void toggleComplete(Integer updatedBy) {
        this.isCompleted = !this.isCompleted;
        this.updatedBy = updatedBy;
    }

    // 완료 상태 설정
    public void setCompleted(Boolean isCompleted, Integer updatedBy) {
        this.isCompleted = isCompleted;
        this.updatedBy = updatedBy;
    }

    // 업무 완료 기록 (실제 수행자, 수행 시간)
    public void completeRecord(String completedByName, LocalTime completedTime, Integer updatedBy) {
        this.completedByName = completedByName;
        this.completedTime = completedTime;
        this.isCompleted = true;
        this.updatedBy = updatedBy;
    }

    // 팩토리 메서드 (담당자 지정)
    public static Task create(String title, User assignee, LocalDate taskDate,
            LocalTime taskTime, CodeType taskType, Integer createdBy) {
        return Task.builder()
                .title(title)
                .assignee(assignee)
                .taskDate(taskDate)
                .taskTime(taskTime)
                .taskType(taskType)
                .isCompleted(false)
                .createdBy(createdBy)
                .build();
    }

    // 팩토리 메서드 (담당자 미지정)
    public static Task create(String title, LocalDate taskDate,
            LocalTime taskTime, CodeType taskType, Integer createdBy) {
        return Task.builder()
                .title(title)
                .taskDate(taskDate)
                .taskTime(taskTime)
                .taskType(taskType)
                .isCompleted(false)
                .createdBy(createdBy)
                .build();
    }
}
