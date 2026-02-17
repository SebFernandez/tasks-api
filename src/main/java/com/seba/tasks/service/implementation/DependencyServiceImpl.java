package com.seba.tasks.service.implementation;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.error.exceptions.InvalidArgumentException;
import com.seba.tasks.error.exceptions.TaskNotFoundException;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import com.seba.tasks.service.DependencyService;
import com.seba.tasks.utils.TaskUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.seba.tasks.error.ErrorCode.REQUEST_BAD_ATTRIBUTE;
import static com.seba.tasks.error.ErrorCode.TASK_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DependencyServiceImpl implements DependencyService {

    private final TaskRepository taskRepository;

    @Override
    public Mono<TaskDto> addDependency(UUID taskId, UUID blockerTaskId) {
        if (taskId.equals(blockerTaskId)) {
            return Mono.error(new InvalidArgumentException(REQUEST_BAD_ATTRIBUTE, "dependsOn (self-dependency)"));
        }

        Mono<Task> taskMono = taskRepository.findByTaskId(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, taskId)));

        Mono<Task> blockerMono = taskRepository.findByTaskId(blockerTaskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, blockerTaskId)));

        return Mono.zip(taskMono, blockerMono)
                .flatMap(tuple -> {
                    Task task = tuple.getT1();
                    Task blocker = tuple.getT2();

                    if (task.getDependsOn().contains(blockerTaskId)) {
                        return Mono.just(TaskUtility.toTaskDto(task));
                    }

                    List<UUID> updatedDeps = new ArrayList<>(task.getDependsOn());
                    updatedDeps.add(blockerTaskId);
                    task.setDependsOn(updatedDeps);

                    if (blocker.getStatus() != TaskStatus.DONE) {
                        task.setStatus(TaskStatus.BLOCKED);
                    }

                    return taskRepository.save(task).map(TaskUtility::toTaskDto);
                });
    }

    @Override
    public Mono<TaskDto> removeDependency(UUID taskId, UUID blockerTaskId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
