package com.seba.tasks.service.implementation;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.error.exceptions.TaskBlockedException;
import com.seba.tasks.error.exceptions.TaskNotFoundException;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import com.seba.tasks.service.TaskService;
import com.seba.tasks.utils.TaskUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static com.seba.tasks.error.ErrorCode.TASK_BLOCKED;
import static com.seba.tasks.error.ErrorCode.TASK_NOT_FOUND;

@Service
@RequiredArgsConstructor
public final class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public Mono<TaskDto> create(String title, String createdBy) {
        return taskRepository.save(buildTask(title, createdBy))
                .map(TaskUtility::toTaskDto);
    }

    @Override
    public Mono<TaskDto> getById(UUID taskId) {
        return taskRepository.findByTaskId(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, taskId)))
                .map(TaskUtility::toTaskDto);
    }

    @Override
    public Flux<TaskDto> getAll() {
        return taskRepository.findAll()
                .map(TaskUtility::toTaskDto);
    }

    @Override
    public Mono<TaskDto> update(UUID taskId, String title, TaskStatus status, String updatedBy) {
        return taskRepository.findByTaskId(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, taskId)))
                .flatMap(task -> {
                    if (task.getStatus() == TaskStatus.BLOCKED && status != null)
                        return Mono.error(new TaskBlockedException(TASK_BLOCKED, taskId));

                    if (title != null) task.setTitle(title);
                    if (status != null) task.setStatus(status);
                    task.setUpdatedAt(Instant.now());
                    task.setUpdatedBy(updatedBy);
                    return taskRepository.save(task)
                            .map(TaskUtility::toTaskDto);
                });
    }

    @Override
    public Mono<Void> delete(UUID taskId) {
        return taskRepository.existsByTaskId(taskId)
                .flatMap(exists -> {
                    if (!exists)
                        return Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, taskId));

                    return taskRepository.deleteByTaskId(taskId);
                });
    }

    private static Task buildTask(String title, String createdBy) {
        Task task = new Task();

        task.setTaskId(UUID.randomUUID());
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy(createdBy);
        task.setUpdatedBy(createdBy);
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        return task;
    }
}
