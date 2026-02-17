package com.seba.tasks.service.implementation;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.error.exceptions.CircularDependencyException;
import com.seba.tasks.error.exceptions.DependencyNotFoundException;
import com.seba.tasks.error.exceptions.InvalidArgumentException;
import com.seba.tasks.error.exceptions.TaskNotFoundException;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import com.seba.tasks.service.DependencyService;
import com.seba.tasks.utils.TaskUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.seba.tasks.error.ErrorCode.*;

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

                    return detectCycle(taskId, blockerTaskId)
                            .then(Mono.defer(() -> {
                                List<UUID> updatedDeps = new ArrayList<>(task.getDependsOn());
                                updatedDeps.add(blockerTaskId);
                                task.setDependsOn(updatedDeps);

                                if (blocker.getStatus() != TaskStatus.DONE) {
                                    task.setStatus(TaskStatus.BLOCKED);
                                }

                                return taskRepository.save(task).map(TaskUtility::toTaskDto);
                            }));
                });
    }

    @Override
    public Mono<TaskDto> removeDependency(UUID taskId, UUID blockerTaskId) {
        return taskRepository.findByTaskId(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(TASK_NOT_FOUND, taskId)))
                .flatMap(task -> {
                    if (!task.getDependsOn().contains(blockerTaskId)) {
                        return Mono.error(new DependencyNotFoundException(DEPENDENCY_NOT_FOUND, taskId, blockerTaskId));
                    }

                    List<UUID> updatedDeps = new ArrayList<>(task.getDependsOn());
                    updatedDeps.remove(blockerTaskId);
                    task.setDependsOn(updatedDeps);

                    return recalculateStatus(task)
                            .flatMap(taskRepository::save)
                            .map(TaskUtility::toTaskDto);
                });
    }

    @Override
    public Mono<Void> unblockDependents(UUID completedTaskId) {
        return taskRepository.findByDependsOnContaining(completedTaskId)
                .filter(task -> task.getStatus() == TaskStatus.BLOCKED)
                .flatMap(task ->
                        Flux.fromIterable(task.getDependsOn())
                                .flatMap(taskRepository::findByTaskId)
                                .all(blocker -> blocker.getStatus() == TaskStatus.DONE)
                                .flatMap(allDone -> {
                                    if (allDone) {
                                        task.setStatus(TaskStatus.TODO);
                                        return taskRepository.save(task);
                                    }
                                    return Mono.empty();
                                })
                )
                .then();
    }

    private Mono<Void> detectCycle(UUID taskId, UUID blockerTaskId) {
        return walkDependencies(blockerTaskId, taskId, new HashSet<>());
    }

    private Mono<Void> walkDependencies(UUID currentId, UUID targetId, Set<UUID> visited) {
        if (!visited.add(currentId)) {
            return Mono.empty();
        }

        return taskRepository.findByTaskId(currentId)
                .flatMap(task -> {
                    if (task.getDependsOn().contains(targetId)) {
                        return Mono.error(new CircularDependencyException(CIRCULAR_DEPENDENCY, targetId, currentId));
                    }

                    return Flux.fromIterable(task.getDependsOn())
                            .flatMap(depId -> walkDependencies(depId, targetId, visited))
                            .then();
                });
    }

    private Mono<Task> recalculateStatus(Task task) {
        if (task.getDependsOn().isEmpty()) {
            task.setStatus(TaskStatus.TODO);
            return Mono.just(task);
        }

        return Flux.fromIterable(task.getDependsOn())
                .flatMap(taskRepository::findByTaskId)
                .all(blocker -> blocker.getStatus() == TaskStatus.DONE)
                .map(allDone -> {
                    task.setStatus(allDone ? TaskStatus.TODO : TaskStatus.BLOCKED);
                    return task;
                });
    }
}
