package com.better.CommuteMate.domain.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "todo_completion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_todo_completion_todo_date",
                columnNames = {"todo_id", "completion_date"}
        ),
        indexes = @Index(name = "idx_todo_completion_date", columnList = "completion_date")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_completion_id")
    private Long todoCompletionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(name = "completion_date", nullable = false)
    private LocalDate date;

    @Column(name = "completed_by_name", nullable = false, length = 50)
    private String completedByName;

    @Column(name = "completed_time", nullable = false)
    private LocalTime completedTime;

    @Column(name = "completed_by")
    private Long completedBy;

    public void update(String userName, LocalTime time, Long userId) {
        this.completedByName = userName;
        this.completedTime = time;
        this.completedBy = userId;
    }
}
