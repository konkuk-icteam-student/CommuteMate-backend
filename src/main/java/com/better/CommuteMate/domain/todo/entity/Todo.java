package com.better.CommuteMate.domain.todo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "todo", indexes = {
        @Index(name = "idx_todo_date", columnList = "todo_date"),
        @Index(name = "idx_todo_org", columnList = "organization_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Todo {

    private static final LocalDate RECURRING_TODO_LEGACY_DATE = LocalDate.of(1970, 1, 1);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Long todoId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "todo_date", nullable = false)
    private LocalDate date;

    @Column(name = "todo_time", nullable = false)
    private LocalTime timeSlot;

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

    public void update(LocalTime timeSlot, String description, Long updatedBy) {
        if (timeSlot != null) {
            this.timeSlot = timeSlot;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedBy = updatedBy;
    }

    public void complete(String completedByName, LocalTime completedTime, Long updatedBy) {
        this.isCompleted = true;
        this.completedByName = completedByName;
        this.completedTime = completedTime;
        this.updatedBy = updatedBy;
    }

    public void uncomplete(Long updatedBy) {
        this.isCompleted = false;
        this.completedByName = null;
        this.completedTime = null;
        this.updatedBy = updatedBy;
    }

    public static Todo create(Long organizationId, String description,
            LocalTime timeSlot, Long createdBy) {
        return Todo.builder()
                .organizationId(organizationId)
                .description(description)
                .date(RECURRING_TODO_LEGACY_DATE)
                .timeSlot(timeSlot)
                .isCompleted(false)
                .createdBy(createdBy)
                .build();
    }
}
