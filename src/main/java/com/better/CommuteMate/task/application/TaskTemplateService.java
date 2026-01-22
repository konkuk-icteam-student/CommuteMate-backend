package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.entity.TaskTemplate;
import com.better.CommuteMate.domain.task.entity.TaskTemplateItem;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.task.repository.TaskTemplateItemRepository;
import com.better.CommuteMate.domain.task.repository.TaskTemplateRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.TaskException;
import com.better.CommuteMate.global.exceptions.error.TaskErrorCode;
import com.better.CommuteMate.task.controller.dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskTemplateService {

    private final TaskTemplateRepository templateRepository;
    private final TaskTemplateItemRepository itemRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * 템플릿 목록 조회
     */
    public List<TemplateListResponse> getTemplates(boolean activeOnly) {
        List<TaskTemplate> templates = activeOnly
                ? templateRepository.findByIsActiveTrueOrderByTemplateNameAsc()
                : templateRepository.findAllByOrderByTemplateNameAsc();

        return templates.stream()
                .map(TemplateListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 템플릿 상세 조회 (항목 포함)
     */
    public TemplateDetailResponse getTemplate(Long templateId) {
        TaskTemplate template = findTemplateById(templateId);
        List<TaskTemplateItem> items = itemRepository.findByTemplateIdWithAssignee(templateId);
        return TemplateDetailResponse.from(template, items);
    }

    /**
     * 템플릿 생성
     */
    @Transactional
    public TemplateDetailResponse createTemplate(CreateTemplateRequest request, Long currentUserId) {
        // 이름 중복 확인
        if (templateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new TaskException(TaskErrorCode.TEMPLATE_NAME_ALREADY_EXISTS);
        }

        TaskTemplate template = TaskTemplate.create(
                request.getTemplateName(),
                request.getDescription(),
                currentUserId);

        // 항목 추가
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (int i = 0; i < request.getItems().size(); i++) {
                TemplateItemRequest itemRequest = request.getItems().get(i);
                User defaultAssignee = itemRequest.getDefaultAssigneeId() != null
                        ? findUserById(itemRequest.getDefaultAssigneeId())
                        : null;

                TaskTemplateItem item = TaskTemplateItem.create(
                        itemRequest.getTitle(),
                        defaultAssignee,
                        itemRequest.getTaskTime(),
                        validateAndGetTaskType(itemRequest.getTaskType()),
                        itemRequest.getDisplayOrder() != null ? itemRequest.getDisplayOrder() : i);
                template.addItem(item);
            }
        }

        TaskTemplate savedTemplate = templateRepository.save(template);
        List<TaskTemplateItem> items = itemRepository.findByTemplateIdWithAssignee(savedTemplate.getTemplateId());
        return TemplateDetailResponse.from(savedTemplate, items);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public TemplateDetailResponse updateTemplate(Long templateId, UpdateTemplateRequest request,
                                                 Long currentUserId) {
        TaskTemplate template = findTemplateById(templateId);

        // 이름 중복 확인 (자기 자신 제외)
        if (request.getTemplateName() != null &&
                templateRepository.existsByTemplateNameAndNotId(request.getTemplateName(), templateId)) {
            throw new TaskException(TaskErrorCode.TEMPLATE_NAME_ALREADY_EXISTS);
        }

        template.update(request.getTemplateName(), request.getDescription(), currentUserId);

        // 항목 전체 교체 (있는 경우)
        if (request.getItems() != null) {
            List<TaskTemplateItem> newItems = new ArrayList<>();
            for (int i = 0; i < request.getItems().size(); i++) {
                TemplateItemRequest itemRequest = request.getItems().get(i);
                User defaultAssignee = itemRequest.getDefaultAssigneeId() != null
                        ? findUserById(itemRequest.getDefaultAssigneeId())
                        : null;

                TaskTemplateItem item = TaskTemplateItem.create(
                        itemRequest.getTitle(),
                        defaultAssignee,
                        itemRequest.getTaskTime(),
                        validateAndGetTaskType(itemRequest.getTaskType()),
                        itemRequest.getDisplayOrder() != null ? itemRequest.getDisplayOrder() : i);
                newItems.add(item);
            }
            template.replaceItems(newItems);
        }

        List<TaskTemplateItem> items = itemRepository.findByTemplateIdWithAssignee(templateId);
        return TemplateDetailResponse.from(template, items);
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        TaskTemplate template = findTemplateById(templateId);
        templateRepository.delete(template);
    }

    /**
     * 템플릿 활성화/비활성화
     */
    @Transactional
    public TemplateListResponse setTemplateActive(Long templateId, boolean isActive, Long currentUserId) {
        TaskTemplate template = findTemplateById(templateId);
        template.setActive(isActive, currentUserId);
        return TemplateListResponse.from(template);
    }

    /**
     * 템플릿 적용 (특정 날짜에 업무 일괄 생성)
     */
    @Transactional
    public ApplyTemplateResponse applyTemplate(Long templateId, ApplyTemplateRequest request, Long currentUserId) {
        TaskTemplate template = findTemplateById(templateId);
        List<TaskTemplateItem> items = itemRepository.findByTemplateIdWithAssignee(templateId);

        if (items.isEmpty()) {
            throw new TaskException(TaskErrorCode.TEMPLATE_HAS_NO_ITEMS);
        }

        // 담당자 오버라이드 맵 생성
        Map<Long, Long> assigneeOverrides = request.getAssigneeOverrides() != null
                ? request.getAssigneeOverrides().stream()
                        .collect(Collectors.toMap(
                                AssigneeOverride::getItemId,
                                AssigneeOverride::getAssigneeId))
                : Map.of();

        List<Task> createdTasks = new ArrayList<>();

        for (TaskTemplateItem item : items) {
            // 담당자 결정: 오버라이드 > 기본 담당자
            Long assigneeId = assigneeOverrides.getOrDefault(item.getItemId(),
                    item.getDefaultAssignee() != null ? item.getDefaultAssignee().getUserId() : null);

            if (assigneeId == null) {
                // 담당자가 없으면 건너뛰기 (또는 에러 처리)
                continue;
            }

            User assignee = findUserById(assigneeId);

            Task task = Task.create(
                    item.getTitle(),
                    assignee,
                    request.getTargetDate(),
                    item.getTaskTime(),
                    item.getTaskType(),
                    currentUserId);
            createdTasks.add(task);
        }

        List<Task> savedTasks = taskRepository.saveAll(createdTasks);

        List<TaskResponse> taskResponses = savedTasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        return ApplyTemplateResponse.builder()
                .templateId(templateId)
                .templateName(template.getTemplateName())
                .targetDate(request.getTargetDate())
                .createdCount(savedTasks.size())
                .tasks(taskResponses)
                .build();
    }

    // === Private Helper Methods ===

    private TaskTemplate findTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new TaskException(TaskErrorCode.TEMPLATE_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new TaskException(TaskErrorCode.ASSIGNEE_NOT_FOUND));
    }

    private com.better.CommuteMate.global.code.CodeType validateAndGetTaskType(String taskTypeCode) {
        if (taskTypeCode == null) {
            return com.better.CommuteMate.global.code.CodeType.TT01;
        }
        try {
            com.better.CommuteMate.global.code.CodeType taskType = com.better.CommuteMate.global.code.CodeType
                    .fromFullCode(taskTypeCode);
            if (taskType != com.better.CommuteMate.global.code.CodeType.TT01 &&
                    taskType != com.better.CommuteMate.global.code.CodeType.TT02) {
                throw new TaskException(TaskErrorCode.INVALID_TASK_TYPE);
            }
            return taskType;
        } catch (IllegalArgumentException e) {
            throw new TaskException(TaskErrorCode.INVALID_TASK_TYPE);
        }
    }
}
