package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.TaskException;
import com.better.CommuteMate.global.exceptions.error.TaskErrorCode;
import com.better.CommuteMate.task.controller.dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * 특정 날짜의 업무 목록 조회
     * 정기/비정기, 오전/오후로 구분하여 반환
     */
    public DailyTasksResponse getTasksByDate(LocalDate date) {
        List<Task> tasks = taskRepository.findByTaskDateWithAssignee(date);

        // 정기 업무 (TT01)
        List<Task> regularTasks = tasks.stream()
                .filter(t -> t.getTaskType() == CodeType.TT01)
                .collect(Collectors.toList());

        // 비정기 업무 (TT02)
        List<Task> irregularTasks = tasks.stream()
                .filter(t -> t.getTaskType() == CodeType.TT02)
                .collect(Collectors.toList());

        // 오전/오후 구분 (12시 기준)
        LocalTime noon = LocalTime.of(12, 0);

        List<TaskResponse> morningTasks = regularTasks.stream()
                .filter(t -> t.getTaskTime().isBefore(noon))
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        List<TaskResponse> afternoonTasks = regularTasks.stream()
                .filter(t -> !t.getTaskTime().isBefore(noon))
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        List<TaskResponse> irregularTaskResponses = irregularTasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        return DailyTasksResponse.builder()
                .date(date)
                .regularTasks(RegularTasksResponse.builder()
                        .morning(morningTasks)
                        .afternoon(afternoonTasks)
                        .build())
                .irregularTasks(irregularTaskResponses)
                .build();
    }

    /**
     * 업무 단건 조회
     */
    public TaskResponse getTask(Long taskId) {
        Task task = findTaskById(taskId);
        return TaskResponse.from(task);
    }

    /**
     * 업무 생성 (담당자 미지정)
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long currentUserId) {
        CodeType taskType = validateAndGetTaskType(request.getTaskType());

        Task task = Task.create(
                request.getTitle(),
                request.getTaskDate(),
                request.getTaskTime(),
                taskType,
                currentUserId);

        Task savedTask = taskRepository.save(task);
        return TaskResponse.from(savedTask);
    }

    /**
     * 업무 수정
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long currentUserId) {
        Task task = findTaskById(taskId);

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = findUserById(request.getAssigneeId());
        }

        task.update(request.getTitle(), assignee, request.getTaskTime(), currentUserId);
        return TaskResponse.from(task);
    }

    /**
     * 업무 완료 상태 토글
     */
    @Transactional
    public TaskResponse toggleComplete(Long taskId, Long currentUserId) {
        Task task = findTaskById(taskId);
        task.toggleComplete(currentUserId);
        return TaskResponse.from(task);
    }

    /**
     * 업무 완료 상태 설정
     */
    @Transactional
    public TaskResponse setComplete(Long taskId, Boolean isCompleted, Long currentUserId) {
        Task task = findTaskById(taskId);
        task.setCompleted(isCompleted, currentUserId);
        return TaskResponse.from(task);
    }

    /**
     * 업무 완료 기록 (실제 수행자, 수행 시간)
     */
    @Transactional
    public TaskResponse completeRecord(Long taskId, CompleteRecordRequest request, Long currentUserId) {
        Task task = findTaskById(taskId);
        task.completeRecord(request.getCompletedByName(), request.getCompletedTime(), currentUserId);
        return TaskResponse.from(task);
    }

    /**
     * 업무 삭제
     */
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = findTaskById(taskId);
        taskRepository.delete(task);
    }

    /**
     * 업무 일괄 저장 (생성 + 수정)
     */
    @Transactional
    public BatchUpdateTasksResponse batchUpdateTasks(BatchUpdateTasksRequest request, Long currentUserId) {
        List<TaskResponse> createdTasks = new ArrayList<>();
        List<TaskResponse> updatedTasks = new ArrayList<>();
        List<BatchTaskError> errors = new ArrayList<>();

        for (BatchTaskItem item : request.getTasks()) {
            try {
                if (item.getTaskId() == null) {
                    // 새로운 업무 생성
                    User assignee = findUserById(item.getAssigneeId());
                    CodeType taskType = validateAndGetTaskType(item.getTaskType());

                    Task task = Task.create(
                            item.getTitle(),
                            assignee,
                            request.getDate(),
                            item.getTaskTime(),
                            taskType,
                            currentUserId);
                    Task savedTask = taskRepository.save(task);
                    createdTasks.add(TaskResponse.from(savedTask));
                } else {
                    // 기존 업무 수정
                    Task task = findTaskById(item.getTaskId());
                    User assignee = item.getAssigneeId() != null ? findUserById(item.getAssigneeId()) : null;
                    task.update(item.getTitle(), assignee, item.getTaskTime(), currentUserId);
                    if (item.getIsCompleted() != null) {
                        task.setCompleted(item.getIsCompleted(), currentUserId);
                    }
                    updatedTasks.add(TaskResponse.from(task));
                }
            } catch (TaskException e) {
                errors.add(BatchTaskError.builder()
                        .taskId(item.getTaskId())
                        .title(item.getTitle())
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return BatchUpdateTasksResponse.builder()
                .createdTasks(createdTasks)
                .updatedTasks(updatedTasks)
                .errors(errors)
                .totalCreated(createdTasks.size())
                .totalUpdated(updatedTasks.size())
                .totalErrors(errors.size())
                .build();
    }

    // === Private Helper Methods ===

    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskException(TaskErrorCode.TASK_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new TaskException(TaskErrorCode.ASSIGNEE_NOT_FOUND));
    }

    private CodeType validateAndGetTaskType(String taskTypeCode) {
        if (taskTypeCode == null) {
            return CodeType.TT01; // 기본값: 정기 업무
        }
        try {
            CodeType taskType = CodeType.fromFullCode(taskTypeCode);
            if (taskType != CodeType.TT01 && taskType != CodeType.TT02) {
                throw new TaskException(TaskErrorCode.INVALID_TASK_TYPE);
            }
            return taskType;
        } catch (IllegalArgumentException e) {
            throw new TaskException(TaskErrorCode.INVALID_TASK_TYPE);
        }
    }
}
