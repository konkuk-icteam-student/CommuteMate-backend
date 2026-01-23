package com.better.CommuteMate.domain.task.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, taskTime ASC")
    @Builder.Default
    private List<TaskTemplateItem> items = new ArrayList<>();

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

    // 템플릿 정보 수정
    public void update(String templateName, String description, Long updatedBy) {
        if (templateName != null) {
            this.templateName = templateName;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedBy = updatedBy;
    }

    // 활성화/비활성화
    public void setActive(Boolean isActive, Long updatedBy) {
        this.isActive = isActive;
        this.updatedBy = updatedBy;
    }

    // 항목 추가
    public void addItem(TaskTemplateItem item) {
        items.add(item);
        item.setTemplate(this);
    }

    // 항목 전체 교체
    public void replaceItems(List<TaskTemplateItem> newItems) {
        this.items.clear();
        for (TaskTemplateItem item : newItems) {
            addItem(item);
        }
    }

    // 팩토리 메서드
    public static TaskTemplate create(String templateName, String description, Long createdBy) {
        return TaskTemplate.builder()
                .templateName(templateName)
                .description(description)
                .isActive(true)
                .createdBy(createdBy)
                .build();
    }
}
