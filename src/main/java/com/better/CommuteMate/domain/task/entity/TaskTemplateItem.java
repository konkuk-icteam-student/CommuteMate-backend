package com.better.CommuteMate.domain.task.entity;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "task_template_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TaskTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private TaskTemplate template;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_assignee_id")
    private User defaultAssignee;

    @Column(name = "task_time", nullable = false)
    private LocalTime taskTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType taskType;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // 항목 정보 수정
    public void update(String title, User defaultAssignee, LocalTime taskTime,
            CodeType taskType, Integer displayOrder) {
        if (title != null) {
            this.title = title;
        }
        if (defaultAssignee != null) {
            this.defaultAssignee = defaultAssignee;
        }
        if (taskTime != null) {
            this.taskTime = taskTime;
        }
        if (taskType != null) {
            this.taskType = taskType;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    // 팩토리 메서드
    public static TaskTemplateItem create(String title, User defaultAssignee,
            LocalTime taskTime, CodeType taskType,
            Integer displayOrder) {
        return TaskTemplateItem.builder()
                .title(title)
                .defaultAssignee(defaultAssignee)
                .taskTime(taskTime)
                .taskType(taskType)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
    }
}
